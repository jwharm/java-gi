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

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.util.PartialStatement;
import io.github.jwharm.javagi.util.Platform;

import javax.lang.model.element.Modifier;

import java.util.Collections;
import java.util.List;

import static io.github.jwharm.javagi.util.Conversions.toJavaIdentifier;
import static io.github.jwharm.javagi.util.Conversions.toJavaSimpleType;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;

public class ConstructorGenerator {

    private final Constructor ctor;
    private final RegisteredType parent;
    private final String methodName;
    private final String privateMethodName;

    public ConstructorGenerator(Constructor ctor) {
        this.ctor = ctor;
        parent = ctor.parent();

        privateMethodName = getName(ctor, true);
        methodName = getName(ctor, false);
    }

    public static String getName(Constructor ctor, boolean privateMethodName) {
        String name = ctor.name();
        if (name.startsWith("new_"))
            name = name.substring(4);
        return privateMethodName
                ? toJavaIdentifier("construct_" + name)
                : toJavaIdentifier(name);
    }

    public Iterable<MethodSpec> generate() {
        MethodSpec constructor = ctor.name().equals("new")
                ? constructor()
                : namedConstructor();

        MethodSpec helperMethod = new MethodGenerator(ctor, privateMethodName)
                .generate();

        if (unnamedConstructorWithOneNullableArg()) {
            MethodSpec overload = generateNoArgConstructor();
            return List.of(constructor, overload, helperMethod);
        } else {
            return List.of(constructor, helperMethod);
        }
    }

    private MethodSpec constructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        // Javadoc
        if (ctor.infoElements().doc() != null) {
            String doc = new DocGenerator(ctor.infoElements().doc()).generate();
            if (ctor.doPlatformCheck())
                builder.addException(ClassNames.UNSUPPORTED_PLATFORM_EXCEPTION)
                       .addJavadoc(doc, ClassNames.UNSUPPORTED_PLATFORM_EXCEPTION);
            else
                builder.addJavadoc(doc);
        }

        // Deprecated annotation
        if (ctor.callableAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        // Parameters
        new CallableGenerator(ctor).generateMethodParameters(builder, false, true);

        // Exception
        if (ctor.callableAttrs().throws_())
            builder.addException(ClassNames.GERROR_EXCEPTION);

        // Invoke private construction method
        // Use this() instead of super() to reinterpret the handle to the size
        // of the memory layout
        builder.addStatement("this(constructNew($L))", javaParameterNames());

        // Cache new GObject instance
        if (parent.checkIsGObject())
            builder.addStatement("$T.put(handle(), this)",
                    ClassNames.INSTANCE_CACHE);

        // Add cleaner to struct/union pointer
        else {
            builder.addStatement("$T.takeOwnership(this)",
                            ClassNames.MEMORY_CLEANER);
            new RegisteredTypeGenerator(parent).setFreeFunc(
                    builder,
                    "this",
                    parent.typeName()
            );
        }

        return builder.build();
    }

    private MethodSpec namedConstructor() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        // Override the specified return type
        TypeName returnType = parent.typeName();
        builder.returns(returnType);

        // Javadoc
        if (ctor.infoElements().doc() != null) {
            String doc = new DocGenerator(ctor.infoElements().doc()).generate();
            if (ctor.doPlatformCheck())
                builder.addJavadoc(doc, ClassNames.UNSUPPORTED_PLATFORM_EXCEPTION);
            else
                builder.addJavadoc(doc);
        }

        // Deprecated annotation
        if (ctor.callableAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        // Parameters
        new CallableGenerator(ctor)
                .generateMethodParameters(builder, false, true);

        // Exception
        if (ctor.callableAttrs().throws_())
            builder.addException(ClassNames.GERROR_EXCEPTION);

        // Platform check
        if (ctor.doPlatformCheck())
            builder.addStatement("$T.checkSupportedPlatform($L)",
                    ClassNames.PLATFORM, Platform.toStringLiterals(ctor.platforms()));

        builder.addStatement("var _result = $L($L)",
                privateMethodName,
                javaParameterNames());

        // Marshal return value and handle ownership transfer
        var generator = new TypedValueGenerator(ctor.returnValue());
        PartialStatement stmt = generator.marshalNativeToJava("_result", false);

        // Ref GObject
        if (parent.checkIsGObject()) {
            builder.addNamedCode(PartialStatement.of("var _object = ")
                                    .add(stmt)
                                    .add(";\n")
                                    .format(),
                            stmt.arguments())
                    .beginControlFlow("if (_object instanceof $T _gobject)",
                            ClassNames.G_OBJECT)
                    .addStatement("$T.debug($S, _gobject.handle().address())",
                            ClassNames.GLIB_LOGGER,
                            "Ref " + returnType + " %ld")
                    .addStatement("_gobject.ref()")
                    .endControlFlow()
                    .addStatement("return ($T) _object", returnType);
        }

        // GVariant constructors return floating references
        else if (ctor.callableAttrs().cIdentifier() != null
                && ctor.callableAttrs().cIdentifier().startsWith("g_variant_new_")) {
            builder.addNamedCode(PartialStatement.of("var _instance = ")
                                    .add(stmt)
                                    .add(";\n")
                                    .format(),
                            stmt.arguments())
                    .beginControlFlow("if (_instance != null)")
                    .addStatement("_instance.refSink()")
                    .addStatement("$T.takeOwnership(_instance)",
                            ClassNames.MEMORY_CLEANER)
                    .addStatement("$T.setFreeFunc(_instance, $S)",
                            ClassNames.MEMORY_CLEANER, "g_variant_unref")
                    .endControlFlow()
                    .addStatement("return ($T) _instance", parent.typeName());
        }

        // Add cleaner to struct/union pointers and non-GObject TypeInstances
        else {
            builder.addNamedCode(PartialStatement.of("var _instance = ")
                                    .add(stmt)
                                    .add(";\n")
                                    .format(),
                            stmt.arguments())
                    .beginControlFlow("if (_instance != null)")
                    .addStatement("$T.takeOwnership(_instance)",
                            ClassNames.MEMORY_CLEANER);

            new RegisteredTypeGenerator(parent)
                    .setFreeFunc(builder, "_instance", returnType);

            builder.endControlFlow()
                   .addStatement("return ($T) _instance", returnType);
        }

        return builder.build();
    }

    private List<Parameter> javaParameters() {
        if (ctor.parameters() == null)
            return Collections.emptyList();

        return ctor.parameters().parameters().stream()
                .filter(not(Parameter::isUserDataParameter))
                .filter(not(Parameter::isDestroyNotifyParameter))
                .filter(not(Parameter::isArrayLengthParameter))
                .toList();
    }

    private String javaParameterNames() {
        return javaParameters().stream()
                .map(TypedValue::name)
                .map(n -> "...".equals(n) ? "varargs" : toJavaIdentifier(n))
                .collect(joining(", "));
    }

    private boolean unnamedConstructorWithOneNullableArg() {
        if (! "new".equals(ctor.name()))
            return false;

        var params = javaParameters();
        return params.size() == 1 && params.getFirst().nullable();
    }

    /*
     * A class with a constructor with one nullable parameter, for example
     * `org.gnome.gtk.Frame#Frame(@Nullable String label)` introduces ambiguity
     * with the default MemorySegment constructor when passing a null argument.
     * For these cases, we add a no-argument constructor.
     */
    private MethodSpec generateNoArgConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        var generator = new TypedValueGenerator(javaParameters().getFirst());
        String className = toJavaSimpleType(ctor.parent().name(), ctor.namespace());

        // Javadoc
        builder.addJavadoc("Calls {@link $1L#$1L($2L)} with $3L = {@code null}",
                className, generator.getType(), generator.getName());

        // Deprecated annotation
        if (ctor.callableAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        // Exception
        if (ctor.callableAttrs().throws_())
            builder.addException(ClassNames.GERROR_EXCEPTION);

        // Call overloaded constructor
        builder.addStatement("this(($T) null)", generator.getType());

        return builder.build();
    }
}
