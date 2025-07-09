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
import org.javagi.gir.*;
import org.javagi.util.GeneratedAnnotationBuilder;
import org.javagi.gir.Class;
import org.javagi.gir.Record;

import javax.lang.model.element.Modifier;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static org.javagi.util.Conversions.getValueLayoutPlain;
import static org.javagi.util.Conversions.toJavaSimpleType;

public class AliasGenerator extends RegisteredTypeGenerator {

    private final Alias alias;
    private final RegisteredType target;

    public AliasGenerator(Alias alias) {
        super(alias);
        this.alias = alias;
        this.target = alias.lookup();
    }

    public TypeSpec generate() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(alias.typeName());
        builder.addAnnotation(GeneratedAnnotationBuilder.generate());

        // Alias for an alias for a primitive type
        if (target instanceof Alias other && other.isValueWrapper())
            builder.superclass(other.typeName())
                    .addMethod(valueConstructor(other.anyType().typeName()))
                    .addMethod(arrayConstructor(other.anyType()));

        // Alias for an alias
        else if (target instanceof Alias other)
            builder.superclass(other.typeName())
                    .addMethod(memoryAddressConstructor());

        else if (target instanceof Class || target instanceof Record
                || target instanceof Boxed || target instanceof Union)
            builder.superclass(target.typeName())
                    .addMethod(memoryAddressConstructor());

        else if (target instanceof Interface || target instanceof Callback)
            builder = TypeSpec.interfaceBuilder(alias.typeName())
                    .addSuperinterface(target.typeName());

        else if (alias.anyType().isVoid()) {
            builder.superclass(ParameterizedTypeName.get(
                            ClassNames.ALIAS, TypeName.get(MemorySegment.class)))
                    .addMethod(valueConstructor(TypeName.get(MemorySegment.class)));
        }

        else if (alias.isValueWrapper())
            builder.superclass(ParameterizedTypeName.get(
                            ClassNames.ALIAS, alias.anyType().typeName().box()))
                    .addMethod(valueConstructor(alias.anyType().typeName()))
                    .addMethod(arrayConstructor(alias.anyType()));

        if (target instanceof Class || target instanceof Interface
                || target instanceof Record || target instanceof Boxed
                || target instanceof Union)
            builder.addMethod(fromTargetType());

        if (alias.infoElements().doc() != null)
            builder.addJavadoc(
                    new DocGenerator(alias.infoElements().doc()).generate());
        if (alias.infoAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        if (alias.toStringTarget() != null)
            builder.addMethod(toStringRedirect());

        return builder.addModifiers(Modifier.PUBLIC).build();
    }

    private MethodSpec valueConstructor(TypeName typeName) {
        return MethodSpec.constructorBuilder()
                .addJavadoc("Create a new $L with the provided value",
                        toJavaSimpleType(alias.name(), alias.namespace()))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(typeName, "value")
                .addStatement("super(value)")
                .build();
    }

    @Override
    protected MethodSpec memoryAddressConstructor() {
        var spec = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("""
                    Create a $L proxy instance for the provided memory address.
                    
                    @param address the memory address of the native object
                    """, name())
                .addParameter(MemorySegment.class, "address")
                .addStatement("super(address)");
        return spec.build();
    }

    private MethodSpec fromTargetType() {
        String name = "from" + toJavaSimpleType(target.name(), target.namespace());
        return MethodSpec.methodBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("""
                    Cast a $1L instance to a $2L with the same memory address.
                    
                    @param  alias the $1L to cast to a $2L
                    @return a $2L with the memory address of the $1L
                    """, target.name(), name())
                .returns(alias.typeName())
                .addParameter(target.typeName(), "alias")
                .addStatement("return new $T(alias.handle())",
                        alias.typeName())
                .build();
    }

    private MethodSpec arrayConstructor(AnyType anyType) {
        String layout = switch (anyType) {
            case Array _ -> "ADDRESS";
            case Type type -> getValueLayoutPlain(type, false);
        };

        var spec = MethodSpec.methodBuilder("fromNativeArray")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ArrayTypeName.of(alias.typeName()))
                .addParameter(MemorySegment.class, "address")
                .addParameter(long.class, "length")
                .addParameter(ClassNames.TRANSFER_OWNERSHIP, "transfer")
                .addStatement("$T array = new $T[(int) length]",
                        ArrayTypeName.of(alias.typeName()), alias.typeName());

        if (anyType instanceof Type t && t.isLong()) {
            spec.addStatement("long byteSize = $1T.longAsInt() ? $2T.JAVA_INT.byteSize() : $2T.JAVA_LONG.byteSize()",
                    ClassNames.INTEROP, ValueLayout.class);
        } else {
            spec.addStatement("long byteSize = $T.$L.byteSize()",
                    ValueLayout.class, layout);
        }

        spec.addStatement("$T segment = address.reinterpret(byteSize * length)",
                        MemorySegment.class)
                .beginControlFlow("for (int i = 0; i < length; i++)");

        // String[]
        if (anyType instanceof Array a
                && a.anyType() instanceof Type t
                && t.typeName().equals(TypeName.get(String.class)))
            spec.addStatement("array[i] = new $T($T.getStringArrayFrom(segment.get($T.$L, i * byteSize), transfer))",
                    alias.typeName(),
                    ClassNames.INTEROP,
                    ValueLayout.class,
                    layout);
        // String
        else if (anyType instanceof Type t
                && t.typeName().equals(TypeName.get(String.class)))
            spec.addStatement("array[i] = new $T($T.getStringFrom(segment.get($T.$L, i * byteSize), transfer))",
                    alias.typeName(),
                    ClassNames.INTEROP,
                    ValueLayout.class,
                    layout);
        // Primitive value
        else
            spec.addStatement("array[i] = new $T(segment.get($T.$L, i * byteSize))",
                    alias.typeName(),
                    ValueLayout.class,
                    layout);

        return spec.endControlFlow()
                .addStatement("if (transfer != $T.NONE) $T.free(address)",
                        ClassNames.TRANSFER_OWNERSHIP,
                        ClassNames.G_LIB)
                .addStatement("return array")
                .build();
    }
}
