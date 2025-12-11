/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 the Java-GI developers
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package org.javagi.generators;

import com.squareup.javapoet.*;
import org.javagi.configuration.ClassNames;
import org.javagi.gir.Bitfield;
import org.javagi.gir.EnumType;
import org.javagi.gir.Member;
import org.javagi.util.GeneratedAnnotationBuilder;
import org.javagi.util.Numbers;

import javax.lang.model.element.Modifier;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.javagi.util.Conversions.toJavaConstantUpperCase;
import static org.javagi.util.Conversions.toJavaSimpleType;
import static java.util.function.Predicate.not;

public class EnumGenerator extends RegisteredTypeGenerator {

    private final EnumType en;
    private final TypeSpec.Builder builder;

    public EnumGenerator(EnumType en) {
        super(en);
        this.en = en;
        this.builder = TypeSpec.enumBuilder(en.typeName());
        this.builder.addAnnotation(GeneratedAnnotationBuilder.generate());
    }

    public TypeSpec generate() {
        if (en.infoElements().doc() != null)
            builder.addJavadoc(
                    new DocGenerator(en.infoElements().doc()).generate());
        if (en.infoAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        builder.addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassNames.ENUMERATION)
                .addStaticBlock(staticBlock())
                .addField(TypeName.INT, "value", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(valueConstructor())
                .addMethod(staticConstructor())
                .addMethod(getValueMethod());

        if (en instanceof Bitfield)
            builder.addMethod(staticFlagsConstructor())
                   .addMethod(addressSetConstructor());
        else
            builder.addMethod(addressConstructor());

        if (hasTypeMethod())
            builder.addMethod(getTypeMethod());

        addFunctions(builder);
        addMethods(builder);

        if (en.toStringTarget() != null)
            builder.addMethod(toStringRedirect());

        List<Member> uniques = filterDuplicateValues(en.members());
        for (Member m : uniques) {
            try {
                // Enum value
                TypeSpec.Builder spec = TypeSpec.anonymousClassBuilder(
                        "$L", Numbers.parseInt(m.value()));

                // Javadoc
                if (m.infoElements().doc() != null)
                    spec.addJavadoc(
                            new DocGenerator(m.infoElements().doc()).generate());

                // Deprecated annotation
                if (m.callableAttrs().deprecated())
                    spec.addAnnotation(Deprecated.class);

                // Build the enum constant
                builder.addEnumConstant(toJavaConstantUpperCase(m.name()), spec.build());
            } catch (NumberFormatException nfe) {
                log(m);
            }
        }

        List<Member> duplicates = en.members().stream()
                .filter(not(uniques::contains))
                .toList();
        for (Member m : duplicates) {
            try {
                // Static field with constant value
                var spec = FieldSpec.builder(
                                en.typeName(),
                                toJavaConstantUpperCase(m.name()),
                                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

                // Parse the value
                int value = Numbers.parseInt(m.value());
                if (en instanceof Bitfield)
                    spec.initializer("ofFlag($L)", value);
                else
                    spec.initializer("of($L)", value);

                // Javadoc
                if (m.infoElements().doc() != null)
                    spec.addJavadoc(
                            new DocGenerator(m.infoElements().doc()).generate());

                // Deprecated annotation
                if (m.callableAttrs().deprecated())
                    spec.addAnnotation(Deprecated.class);

                // Build the static field
                builder.addField(spec.build());
            } catch (NumberFormatException nfe) {
                log(m);
            }
        }

        if (hasDowncallHandles())
            builder.addType(downcallHandlesClass());

        return builder.build();
    }

    private MethodSpec valueConstructor() {
        return MethodSpec.constructorBuilder()
                .addJavadoc("""
                    Create a new $L for the provided value
                    
                    @param value the $L value
                    """, toJavaSimpleType(en.name(), en.namespace()),
                         en instanceof Bitfield ? "bitfield" : "enum")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(TypeName.INT, "value")
                .addStatement("this.value = value")
                .build();
    }

    private MethodSpec staticConstructor() {
        MethodSpec.Builder spec;
        if (en instanceof Bitfield) {
            spec = MethodSpec.methodBuilder("ofFlag")
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC);
        } else {
            spec = MethodSpec.methodBuilder("of")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        }
        spec.addJavadoc("""
                        Create a new $1L for the provided value
                        
                        @param value the $2L value
                        @return the $2L for the provided value
                        """, toJavaSimpleType(en.name(), en.namespace()),
                             en instanceof Bitfield ? "bitfield" : "enum")
                .returns(en.typeName())
                .addParameter(TypeName.INT, "value")
                .beginControlFlow("return switch(value)");
        for (Member m : filterDuplicateValues(en.members())) {
            try {
                spec.addStatement("case $L -> $L",
                        Numbers.parseInt(m.value()),
                        toJavaConstantUpperCase(m.name()));
            } catch (NumberFormatException nfe) {
                log(m);
            }
        }
        return spec.addStatement("default -> throw new $T($S + value)",
                        IllegalStateException.class, "Unexpected value: ")
                .endControlFlow("") // empty string to force a ";"
                .build();
    }

    private MethodSpec staticFlagsConstructor() {
        return MethodSpec.methodBuilder("of")
                .addJavadoc("""
                        Create a new {@code EnumSet<$1L>} for the provided bitfield
                        
                        @param flags the $1L bitfield
                        @return the EnumSet for the provided bitfield
                        """, toJavaSimpleType(en.name(), en.namespace()))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Set.class), en.typeName()))
                .addParameter(TypeName.INT, "flags")
                .addStatement("return $1T.intToEnumSet($2T.class, $2T::ofFlag, flags)",
                        ClassNames.INTEROP, en.typeName())
                .build();
    }

    private MethodSpec addressConstructor() {
        return MethodSpec.methodBuilder("of")
                .addJavadoc("""
                        Create a new $1L for the value in the provided memory address.
                        
                        @param address the memory address holding a $2L value
                        @return the $2L for the value in the provided memory address
                        """, toJavaSimpleType(en.name(), en.namespace()),
                        en instanceof Bitfield ? "bitfield" : "enum")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(en.typeName())
                .addParameter(MemorySegment.class, "address")
                .addStatement("return of((int) address.address())")
                .build();
    }

    private MethodSpec addressSetConstructor() {
        return MethodSpec.methodBuilder("of")
                .addJavadoc("""
                        Create a new {@code EnumSet<$1L>} for the bitfield
                        in the provided memory address.
                        
                        @param address the memory address holding a bitfield value
                        @return the EnumSet for the bitfield in the provided memory address
                        """, toJavaSimpleType(en.name(), en.namespace()))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Set.class), en.typeName()))
                .addParameter(MemorySegment.class, "address")
                .addStatement("return $1T.intToEnumSet($2T.class, $2T::ofFlag, (int) address.address())",
                        ClassNames.INTEROP, en.typeName())
                .build();
    }

    private MethodSpec getValueMethod() {
        return MethodSpec.methodBuilder("getValue")
                .addJavadoc("""
                        Get the numeric value of this enum
                        
                        @return the enum value
                        """)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.INT)
                .addStatement("return value")
                .build();
    }

    private static List<Member> filterDuplicateValues(List<Member> input) {
        HashSet<String> set = new HashSet<>();
        ArrayList<Member> output = new ArrayList<>();
        for (Member m : input)
            if (!set.contains(m.value())) {
                output.add(m);
                set.add(m.value());
            }
        return output;
    }

    private static void log(Member m) {
        System.out.printf("Skipping enum member %s: \"%s\" is not an integer%n",
                m.cIdentifier(), m.value());
    }
}
