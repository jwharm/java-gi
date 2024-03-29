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
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.Conversions;
import io.github.jwharm.javagi.util.PartialStatement;
import io.github.jwharm.javagi.util.Platform;

import javax.lang.model.element.Modifier;

import java.util.List;
import java.util.stream.Collectors;

import static io.github.jwharm.javagi.util.Conversions.toJavaIdentifier;
import static java.util.function.Predicate.not;

public class ConstructorGenerator {

    private final Constructor ctor;
    private final RegisteredType parent;
    private final String methodName;
    private final String privateMethodName;

    public ConstructorGenerator(Constructor ctor) {
        this.ctor = ctor;
        parent = ctor.parent();

        // Method name, without "new" prefix
        String name = ctor.name();
        if (name.startsWith("new_"))
            name = name.substring(4);
        privateMethodName = toJavaIdentifier("construct_" + name);
        methodName = toJavaIdentifier(name);
    }

    public Iterable<MethodSpec> generate() {
        return List.of(
                ctor.name().equals("new")
                        ? constructor()
                        : namedConstructor(),
                new MethodGenerator(ctor, privateMethodName).generate());
    }

    private MethodSpec constructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        // Javadoc
        if (ctor.infoElements().doc() != null) {
            String javadoc = new DocGenerator(ctor.infoElements().doc()).generate();
            if (ctor.doPlatformCheck())
                builder.addException(ClassNames.UNSUPPORTED_PLATFORM_EXCEPTION)
                        .addJavadoc(javadoc, ClassNames.UNSUPPORTED_PLATFORM_EXCEPTION);
            else
                builder.addJavadoc(javadoc);
        }

        // Deprecated annotation
        if (ctor.callableAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        // Parameters
        new CallableGenerator(ctor).generateMethodParameters(builder);

        // Exception
        if (ctor.callableAttrs().throws_())
            builder.addException(ClassNames.GERROR_EXCEPTION);

        // Invoke private construction method
        builder.addStatement("super(constructNew($L))", parameterNames());

        // Cache new instance
        if (parent instanceof Class || parent instanceof Interface)
            builder.addStatement("$T.put(handle(), this)", ClassNames.INSTANCE_CACHE);

        return builder.build();
    }

    private MethodSpec namedConstructor() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        // Override the specified return type
        builder.returns(parent.typeName());

        // Javadoc
        if (ctor.infoElements().doc() != null) {
            String javadoc = new DocGenerator(ctor.infoElements().doc()).generate();
            if (ctor.doPlatformCheck())
                builder.addJavadoc(javadoc, ClassNames.UNSUPPORTED_PLATFORM_EXCEPTION);
            else
                builder.addJavadoc(javadoc);
        }

        // Deprecated annotation
        if (ctor.callableAttrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        // Parameters
        new CallableGenerator(ctor).generateMethodParameters(builder);

        // Exception
        if (ctor.callableAttrs().throws_())
            builder.addException(ClassNames.GERROR_EXCEPTION);

        // Platform check
        if (ctor.doPlatformCheck())
            builder.addStatement("$T.checkSupportedPlatform($L)",
                    ClassNames.PLATFORM, Platform.toStringLiterals(ctor.platforms()));

        builder.addStatement("var _result = $L($L)", privateMethodName, parameterNames());

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
                            ClassNames.GOBJECT)
                    .addStatement("$T.debug($S, _gobject.handle())",
                            ClassNames.GLIB_LOGGER,
                            "Ref " + parent.typeName() + " %ld\\n")
                    .addStatement("_gobject.ref()")
                    .endControlFlow()
                    .addStatement("return ($T) _object", parent.typeName());
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
                    .addStatement("$T.takeOwnership(_instance.handle())",
                            ClassNames.MEMORY_CLEANER)
                    .addStatement("$T.setFreeFunc(_instance.handle(), $S)",
                            ClassNames.MEMORY_CLEANER, "g_variant_unref")
                    .endControlFlow()
                    .addStatement("return ($T) _instance", parent.typeName());
        }

        // Add cleaner to struct/union pointer
        else if (parent instanceof Record record) {
            builder.addNamedCode(PartialStatement.of("var _instance = ")
                                    .add(stmt)
                                    .add(";\n")
                                    .format(),
                            stmt.arguments())
                    .beginControlFlow("if (_instance != null)")
                    .addStatement("$T.takeOwnership(_instance.handle())",
                            ClassNames.MEMORY_CLEANER);

            new RecordGenerator(record).setFreeFunc(
                    builder,
                    "_instance",
                    parent.typeName()
            );

            builder.endControlFlow()
                    .addStatement("return ($T) _instance",
                            parent.typeName());
        }

        // No ownership transfer, just marshal the return value
        else {
            stmt = PartialStatement.of("return ($parentType:T) ",
                            "parentType", parent.typeName())
                    .add(stmt)
                    .add(";\n");
            builder.addNamedCode(stmt.format(), stmt.arguments());
        }

        return builder.build();
    }

    private String parameterNames() {
        if (ctor.parameters() == null)
            return "";

        return ctor.parameters().parameters().stream()
                .filter(not(Parameter::isUserDataParameter))
                .filter(not(Parameter::isDestroyNotifyParameter))
                .filter(not(Parameter::isArrayLengthParameter))
                .map(TypedValue::name)
                .map(name -> "...".equals(name)
                        ? "varargs"
                        : Conversions.toJavaIdentifier(name))
                .collect(Collectors.joining(", "));
    }
}
