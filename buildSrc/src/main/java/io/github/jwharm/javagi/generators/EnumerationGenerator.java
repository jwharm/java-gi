/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 the Java-GI developers
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

package io.github.jwharm.javagi.generators;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.gir.Enumeration;
import io.github.jwharm.javagi.gir.Member;
import io.github.jwharm.javagi.util.Numbers;

import javax.lang.model.element.Modifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static io.github.jwharm.javagi.util.Conversions.toJavaConstant;
import static io.github.jwharm.javagi.util.Conversions.toJavaSimpleType;
import static java.util.function.Predicate.not;

public class EnumerationGenerator extends RegisteredTypeGenerator {

    private final Enumeration en;
    private final TypeSpec.Builder builder;

    public EnumerationGenerator(Enumeration en) {
        super(en);
        this.en = en;
        this.builder = TypeSpec.enumBuilder(en.typeName());
    }

    public TypeSpec generate() {
        if (en.infoElements().doc() != null) builder.addJavadoc(new DocGenerator(en.infoElements().doc()).generate());
        if (en.attrs().deprecated()) builder.addAnnotation(Deprecated.class);

        builder.addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassNames.ENUMERATION)
                .addStaticBlock(staticBlock())
                .addField(TypeName.INT, "value", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(valueConstructor())
                .addMethod(staticConstructor())
                .addMethod(getValueMethod());

        if (hasTypeMethod()) builder.addMethod(getTypeMethod());

        addFunctions(builder);
        addMethods(builder);

        List<Member> uniques = filterDuplicateValues(en.members());
        for (Member m : uniques) {
            try {
                TypeSpec.Builder spec = TypeSpec.anonymousClassBuilder("$L", Numbers.parseInt(m.value()));
                if (m.infoElements().doc() != null)
                    spec.addJavadoc(new DocGenerator(m.infoElements().doc()).generate());
                builder.addEnumConstant(m.name().toUpperCase(), spec.build());
            } catch (NumberFormatException nfe) {
                System.out.printf("Skipping enum member %s: \"%s\" is not an integer%n", m.cIdentifier(), m.value());
            }
        }
        for (Member m : en.members().stream().filter(not(uniques::contains)).toList()) {
            try {
                var spec = FieldSpec.builder(en.typeName(), toJavaConstant(m.name()),
                                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("of($L)", Numbers.parseInt(m.value()));
                if (m.infoElements().doc() != null)
                    spec.addJavadoc(new DocGenerator(m.infoElements().doc()).generate());
                builder.addField(spec.build());
            } catch (NumberFormatException nfe) {
                System.out.printf("Skipping enum member %s: \"%s\" is not an integer%n", m.cIdentifier(), m.value());
            }
        }

        return builder.build();
    }

    private MethodSpec valueConstructor() {
        return MethodSpec.constructorBuilder()
                .addJavadoc("""
                    Create a new $L for the provided value
                    @param value the enum value
                    """, toJavaSimpleType(en.name(), en.namespace()))
                .addModifiers(Modifier.PRIVATE)
                .addParameter(TypeName.INT, "value")
                .addStatement("this.value = value;")
                .build();
    }

    private MethodSpec staticConstructor() {
        MethodSpec.Builder spec = MethodSpec.methodBuilder("of")
                .addJavadoc("""
                        Create a new $L for the provided value
                        @param value the enum value
                        @return the enum for the provided value
                        """, toJavaSimpleType(en.name(), en.namespace()))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(en.typeName())
                .addParameter(TypeName.INT, "value")
                .beginControlFlow("return switch(value)");
        for (Member m : filterDuplicateValues(en.members()))
            spec.addStatement("case $L -> $L", m.value(), m.name().toUpperCase());
        return spec.addStatement("default -> throw new IllegalStateException($S + value)", "Unexpected value: ")
                .endControlFlow("") // empty string to force an ;
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
}
