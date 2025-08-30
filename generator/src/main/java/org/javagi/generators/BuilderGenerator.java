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
import org.javagi.util.PartialStatement;
import org.javagi.gir.Class;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Modifier;

import java.lang.foreign.Arena;
import java.util.EnumSet;

import static org.javagi.util.Conversions.toCamelCase;
import static java.util.function.Predicate.not;

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
                        with the specified properties.
                        Use the various {@code set...()} methods to set properties,
                        and finish construction with {@link Builder#build()}.
                        """, rt.typeName().simpleName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(
                            builderTypeName,
                            WildcardTypeName.subtypeOf(builderTypeName)))
                .addStatement("return new $T<>()", builderTypeName)
                .build();
    }

    public TypeSpec generateBuilderClass() {
        Class cls = (Class) rt;
        Class parentClass = cls.parentClass();
        ClassName parentTypeName = parentClass == null ? ClassNames.BUILDER
                : parentClass.typeName().nestedClass("Builder");

        return TypeSpec.classBuilder("Builder")
                .addJavadoc("""
                        Inner class implementing a builder pattern to construct a GObject with
                        properties.
                        
                        @param <B> the type of the Builder that is returned
                        """)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addTypeVariable(BUILDER_B)
                .superclass(ParameterizedTypeName.get(parentTypeName, B))
                .addSuperinterfaces(cls.implements_().stream()
                        .map(TypeReference::lookup)
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
                        .filter(not(Property::skipJava))
                        .filter(Property::writable)
                        .map(this::setter)
                        ::iterator)
                .addMethods(cls.properties().stream()
                        .filter(not(Property::skipJava))
                        .filter(Property::writable)
                        .filter(TypedValue::isBitfield)
                        .map(this::setterVarargs)
                        ::iterator)
                .addMethods(cls.signals().stream()
                        .map(this::connector)
                        ::iterator)
                .build();
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
                .addMethods(((Interface) rt).properties().stream()
                        .filter(Property::writable)
                        .filter(TypedValue::isBitfield)
                        .map(this::setterVarargs)
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
            builder.addJavadoc(
                    new DocGenerator(prp.infoElements().doc()).generate());

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
                .addStatement("$1T _value = new $1T(_arena)", ClassNames.G_VALUE)
                .addNamedCode("_value.init(" + gtype.format() + ");\n", gtype.arguments())
                .addNamedCode(valueSetter.format(), valueSetter.arguments())
                .addStatement("addBuilderProperty($S, _value)", prp.name())
                .addStatement("return (B) this")
                .build();
    }

    // Generates a second setter method for bitfield (flag) properties, where
    // the argument is a vararg instead of a Set<>. It redirects to the original
    // setter.
    private MethodSpec setterVarargs(Property prp) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(
                        "set" + toCamelCase(prp.name(), true))
                .addModifiers(Modifier.PUBLIC)
                .returns(BUILDER_B);

        TypedValueGenerator generator = new TypedValueGenerator(prp);

        // Javadoc
        if (prp.infoElements().doc() != null)
            builder.addJavadoc(
                    new DocGenerator(prp.infoElements().doc()).generate());

        // Deprecated annotation
        if (prp.infoAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        // Modifiers
        if (prp.parent() instanceof Interface)
            builder.addModifiers(Modifier.DEFAULT);

        // Parameter
        var type = generator.getType(false);
        var name = generator.getName();
        builder.addParameter(ArrayTypeName.of(type), name);
        builder.varargs(true);

        // Method body
        var setter = "set" + toCamelCase(prp.name(), true);
        var param = "(" + name + " == null ? null : (" + name + ".length == 0) ? $enumSet:T.noneOf($typeName:T.class) : $enumSet:T.of(" + name + "[0], " + name + "))";
        var stmt = PartialStatement.of("return " + setter + "(" + param + ");",
                "typeName", type,
                "enumSet", EnumSet.class);
        return builder.addNamedCode(stmt.format(), stmt.arguments()).build();
    }

    private MethodSpec connector(Signal signal) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(
                "on" + toCamelCase(signal.name(), true))
                .addModifiers(Modifier.PUBLIC)
                .returns(BUILDER_B);

        // Javadoc
        if (signal.infoElements().doc() != null)
            builder.addJavadoc(
                    new DocGenerator(signal.infoElements().doc()).generate());
        builder.addJavadoc("""
                
                @param  handler the signal handler
                @return the {@code Builder} instance is returned, to allow method chaining
                @see    $LCallback#run
                """, toCamelCase(signal.name(), true));

        // Deprecated annotation
        if (signal.infoAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        // Modifiers
        if (signal.parent() instanceof Interface)
            builder.addModifiers(Modifier.DEFAULT);

        // Parameters
        if (signal.detailed())
            builder.addParameter(
                    ParameterSpec.builder(String.class, "detail")
                            .addAnnotation(Nullable.class)
                            .build());

        builder.addParameter(signal.typeName(), "handler");

        // Method body
        if (signal.detailed())
            builder.addStatement("connect($S, detail, handler)", signal.name());
        else
            builder.addStatement("connect($S, handler)", signal.name());
        builder.addStatement("return (B) this");

        return builder.build();
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
                        """, rt.typeName().simpleName(), ClassNames.G_OBJECT);

        builder.beginControlFlow("try");

        return builder.addStatement("var _instance = ($1T) $2T.withProperties($1T.getType(), getNames(), getValues())",
                        rt.typeName(),
                        ClassNames.G_OBJECT)
                .addStatement("connectSignals(_instance.handle())")
                .addStatement("return _instance")
                .nextControlFlow("finally")
                .addStatement("for ($T _value : getValues()) _value.unset()",
                        ClassNames.G_VALUE)
                .addStatement("getArena().close()")
                .endControlFlow()
                .build();
    }
}
