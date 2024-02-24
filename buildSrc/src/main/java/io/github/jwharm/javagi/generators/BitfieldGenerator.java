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

import com.squareup.javapoet.*;
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.gir.Bitfield;
import io.github.jwharm.javagi.gir.Member;
import io.github.jwharm.javagi.util.GeneratedAnnotationBuilder;
import io.github.jwharm.javagi.util.Numbers;

import javax.lang.model.element.Modifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static io.github.jwharm.javagi.util.Conversions.toJavaConstantUpperCase;
import static io.github.jwharm.javagi.util.Conversions.toJavaSimpleType;

public class BitfieldGenerator extends RegisteredTypeGenerator {

    private final Bitfield bf;
    private final TypeSpec.Builder builder;

    public BitfieldGenerator(Bitfield bf) {
        super(bf);
        this.bf = bf;
        this.builder = TypeSpec.classBuilder(bf.typeName());
        this.builder.addAnnotation(GeneratedAnnotationBuilder.generate());
    }

    public TypeSpec generate() {
        if (bf.infoElements().doc() != null)
            builder.addJavadoc(new DocGenerator(bf.infoElements().doc()).generate());
        if (bf.infoAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        builder.addModifiers(Modifier.PUBLIC)
                .superclass(ClassNames.BITFIELD)
                .addStaticBlock(staticBlock())
                .addMethod(valueConstructor())
                .addMethod(orMethod())
                .addMethod(combineMethod());

        if (hasTypeMethod())
            builder.addMethod(getTypeMethod());

        addFunctions(builder);
        addMethods(builder);

        for (Member m : filterDuplicateNames(bf.members())) {
            try {
                var spec = FieldSpec.builder(bf.typeName(),
                                toJavaConstantUpperCase(m.name()),
                                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T($L)", bf.typeName(), Numbers.parseInt(m.value()));
                if (m.infoElements().doc() != null)
                    spec.addJavadoc(new DocGenerator(m.infoElements().doc()).generate());
                builder.addField(spec.build());
            } catch (NumberFormatException nfe) {
                System.out.printf("Skipping bitfield member %s: \"%s\" is not an integer%n",
                        m.cIdentifier(), m.value());
            }
        }

        return builder.build();
    }

    private MethodSpec valueConstructor() {
        return MethodSpec.constructorBuilder()
                .addJavadoc("Create a new $L with the provided value",
                        toJavaSimpleType(bf.name(), bf.namespace()))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.INT, "value")
                .addStatement("super(value)")
                .build();
    }

    private MethodSpec orMethod() {
        return MethodSpec.methodBuilder("or")
                .addJavadoc("""
                        Combine (bitwise OR) operation
                        
                        @param masks one or more values to combine with
                        @return the combined value by calculating {@code this | mask}
                        """)
                .addModifiers(Modifier.PUBLIC)
                .returns(bf.typeName())
                .addParameter(ArrayTypeName.of(bf.typeName()), "masks")
                .varargs()
                .addStatement("int value = this.getValue()")
                .beginControlFlow("for ($T arg : masks)", bf.typeName())
                .addStatement("value |= arg.getValue()")
                .endControlFlow()
                .addStatement("return new $T(value)", bf.typeName())
                .build();
    }

    private MethodSpec combineMethod() {
        return MethodSpec.methodBuilder("combined")
                .addJavadoc("""
                        Combine (bitwise OR) operation
                        
                        @param mask the first value to combine
                        @param masks the other values to combine
                        @return the combined value by calculating {@code mask | masks[0] | masks[1] | ...}
                        """)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(bf.typeName())
                .addParameter(bf.typeName(), "mask")
                .addParameter(ArrayTypeName.of(bf.typeName()), "masks")
                .varargs()
                .addStatement("int value = mask.getValue()")
                .beginControlFlow("for ($T arg : masks)", bf.typeName())
                .addStatement("value |= arg.getValue()")
                .endControlFlow()
                .addStatement("return new $T(value)", bf.typeName())
                .build();
    }

    private static List<Member> filterDuplicateNames(List<Member> input) {
        HashSet<String> set = new HashSet<>();
        ArrayList<Member> output = new ArrayList<>();
        for (Member m : input) {
            if (!set.contains(m.name())) {
                output.add(m);
                set.add(m.name());
            }
        }
        return output;
    }
}
