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
import io.github.jwharm.javagi.util.PartialStatement;
import io.github.jwharm.javagi.util.Platform;

import javax.lang.model.element.Modifier;

import java.lang.foreign.Arena;

import static io.github.jwharm.javagi.util.Conversions.toCamelCase;

public class BuilderGenerator {

    private final TypeVariableName B = TypeVariableName.get("B");
    private final TypeVariableName BUILDER_B;
    private final RegisteredType rt;

    public BuilderGenerator(RegisteredType rt) {
        this.rt = rt;
        this.BUILDER_B = TypeVariableName.get("B", ParameterizedTypeName.get(switch(rt) {
            case Class cls -> cls.typeName().nestedClass("Builder");
            case Interface _ -> ClassNames.BUILDER;
            default -> throw new IllegalStateException("Only class and interface generate a Builder");
        }, B));
    }

    public MethodSpec generateBuilderMethod() {
        ClassName builderTypeName = rt.typeName().nestedClass("Builder");
        return MethodSpec.methodBuilder("builder")
                .addJavadoc("""
                        A {@link Builder} object constructs a {@code $L}
                        using the <em>builder pattern</em> to set property values.
                        Use the various {@code set...()} methods to set properties,
                        and finish construction with {@link Builder#build()}.
                        """, rt.typeName().simpleName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(builderTypeName, WildcardTypeName.subtypeOf(builderTypeName)))
                .addStatement("return new $T<>()", builderTypeName)
                .build();
    }

    public TypeSpec generateBuilderClass() {
        Class cls = (Class) rt;
        Class parentClass = cls.parentClass();
        ClassName parentTypeName = parentClass == null ? ClassNames.BUILDER
                : parentClass.typeName().nestedClass("Builder");

        TypeSpec.Builder builder = TypeSpec.classBuilder("Builder")
                .addJavadoc("""
                        Inner class implementing a builder pattern to construct a GObject with
                        properties.
                        
                        @param <B> the type of the Builder that is returned
                        """)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariable(BUILDER_B)
                .superclass(ParameterizedTypeName.get(parentTypeName, B))
                .addSuperinterfaces(cls.implements_().stream()
                        .map(TypeReference::get)
                        .map(Interface.class::cast)
                        .filter(Interface::hasProperties)
                        .map(inf -> inf.typeName().nestedClass("Builder"))
                        .map(cn -> ParameterizedTypeName.get(cn, B))
                        .toList())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PROTECTED)
                        .addJavadoc("Default constructor for a {@code Builder} object.")
                        .build())
                .addMethod(buildMethod())
                .addMethods(cls.properties().stream()
                        .filter(Property::writable)
                        .map(this::setter)
                        ::iterator);

        return builder.build();
    }

    public TypeSpec generateBuilderInterface() {
        return TypeSpec.interfaceBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariable(BUILDER_B)
                .addSuperinterface(ClassNames.BUILDER_INTERFACE)
                .addMethods(((Interface) rt).properties().stream()
                        .filter(Property::writable)
                        .map(this::setter)
                        ::iterator)
                .build();
    }

    private MethodSpec setter(Property prp) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(
                "set" + toCamelCase(prp.name(), true))
                .addModifiers(Modifier.PUBLIC)
                .returns(BUILDER_B);

        TypedValueGenerator generator = new TypedValueGenerator(prp);
        PartialStatement gtype = generator.getGTypeDeclaration();

        // Javadoc
        if (prp.infoElements().doc() != null)
            builder.addJavadoc(new DocGenerator(prp.infoElements().doc()).generate());

        // Deprecated annotation
        if (prp.infoAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        // Modifiers
        if (prp.parent() instanceof Interface)
            builder.addModifiers(Modifier.DEFAULT);

        // Parameter
        builder.addParameter(generator.getType(), generator.getName());

        // Method body
        PartialStatement valueSetter = generator.getValueSetter(gtype, generator.getName())
                .add(";\n");
        return builder.addStatement("$T _arena = getArena()", Arena.class)
                .addStatement("$1T _value = $1T.allocate(_arena)",
                        ClassName.get("org.gnome.gobject", "Value"))
                .addNamedCode("_value.init(" + gtype.format() + ");\n", gtype.arguments())
                .addNamedCode(valueSetter.format(), valueSetter.arguments())
                .addStatement("addBuilderProperty($S, _value)", prp.name())
                .addStatement("return (B) this")
                .build();
    }

    private MethodSpec buildMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(rt.typeName())
                .addJavadoc("""
                        Finish building the {@code $1L} object. This will call
                        {@link $2T#withProperties} to create a new GObject instance,
                        which is then cast to {@code $1L}.
                        
                        @return a new instance of {@code $1L} with the properties
                                that were set in the Builder object.
                        """, rt.typeName().simpleName(), ClassNames.GOBJECT);
        if (rt instanceof Multiplatform mp && mp.doPlatformCheck())
            builder.addException(ClassNames.UNSUPPORTED_PLATFORM_EXCEPTION)
                    .addJavadoc("@throws $T when run on an unsupported platform",
                    ClassNames.UNSUPPORTED_PLATFORM_EXCEPTION);

        builder.beginControlFlow("try");

        if (rt instanceof Multiplatform mp && mp.doPlatformCheck())
            builder.addStatement("$T.checkSupportedPlatform($L)",
                    ClassNames.PLATFORM, Platform.toStringLiterals(rt.platforms()));

        return builder.addStatement("return ($1T) $2T.withProperties($1T.getType(), getNames(), getValues())",
                        rt.typeName(), ClassNames.GOBJECT)
                .nextControlFlow("finally")
                .addStatement("for ($T _value : getValues()) _value.unset()", ClassNames.GVALUE)
                .addStatement("getArena().close()")
                .endControlFlow()
                .build();
    }
}
