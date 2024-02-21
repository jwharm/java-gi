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
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.GeneratedAnnotationBuilder;

import javax.lang.model.element.Modifier;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;

import static io.github.jwharm.javagi.util.Conversions.getValueLayoutPlain;
import static io.github.jwharm.javagi.util.Conversions.toJavaSimpleType;

public class AliasGenerator extends RegisteredTypeGenerator {

    private final Alias alias;
    private final RegisteredType target;

    public AliasGenerator(Alias alias) {
        super(alias);
        this.alias = alias;
        this.target = alias.type().get();
    }

    public TypeSpec generate() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(alias.typeName());
        builder.addAnnotation(GeneratedAnnotationBuilder.generate(getClass()));

        // Alias for an alias for a primitive type
        if (target instanceof Alias other && other.type().isPrimitive())
            builder.superclass(other.typeName())
                    .addMethod(valueConstructor(other.type().typeName()))
                    .addMethod(arrayConstructor(other.type()));

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

        else if (alias.type().isPrimitive()
                || List.of("java.lang.String", "java.lang.foreign.MemorySegment").contains(alias.type().javaType()))
            builder.superclass(ParameterizedTypeName.get(ClassNames.ALIAS, alias.type().typeName().box()))
                    .addMethod(valueConstructor(alias.type().typeName()))
                    .addMethod(arrayConstructor(alias.type()));

        if (alias.infoElements().doc() != null)
            builder.addJavadoc(new DocGenerator(alias.infoElements().doc()).generate());
        if (alias.attrs().deprecated())
            builder.addAnnotation(Deprecated.class);

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

    private MethodSpec arrayConstructor(Type primitiveType) {
        String layout = getValueLayoutPlain(primitiveType);
        var spec = MethodSpec.methodBuilder("fromNativeArray")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ArrayTypeName.of(alias.typeName()))
                .addParameter(MemorySegment.class, "address")
                .addParameter(long.class, "length")
                .addParameter(boolean.class, "free")
                .addStatement("$T array = new $T[(int) length]",
                        ArrayTypeName.of(alias.typeName()), alias.typeName())
                .addStatement("long byteSize = $T.$L.byteSize()",
                        ValueLayout.class, layout)
                .addStatement("$T segment = address.reinterpret(byteSize * length)",
                        MemorySegment.class)
                .beginControlFlow("for (int i = 0; i < length; i++)");

        if ("java.lang.String".equals(primitiveType.javaType()))
            spec.addStatement("array[i] = new $T($T.getStringFrom(segment.get($T.$L, i * byteSize), free))",
                    alias.typeName(), ClassNames.INTEROP, ValueLayout.class, layout);
        else
            spec.addStatement("array[i] = new $T(segment.get($T.$L, i * byteSize))",
                    alias.typeName(), ValueLayout.class, layout);

        return spec.endControlFlow()
                .addStatement("if (free) $T.free(address)",
                        ClassName.get("org.gnome.glib", "GLib"))
                .addStatement("return array")
                .build();
    }
}
