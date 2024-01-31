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
import com.squareup.javapoet.TypeSpec;
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.Conversions;
import io.github.jwharm.javagi.util.PartialStatement;
import io.github.jwharm.javagi.util.Platform;

import javax.lang.model.element.Modifier;

import java.util.function.Predicate;
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
        if (name.startsWith("new_")) name = name.substring(4);
        privateMethodName = toJavaIdentifier("construct_" + name);
        methodName = toJavaIdentifier(name);
    }

    public void generate(TypeSpec.Builder classBuilder) {
        classBuilder.addMethod(ctor.name().equals("new") ? constructor() : namedConstructor());
        classBuilder.addMethod(new MethodGenerator(ctor, privateMethodName).generate());
    }

    private MethodSpec constructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        // Javadoc
        if (ctor.infoElements().doc() != null)
            builder.addJavadoc(new DocGenerator(ctor.infoElements().doc()).generate());

        // Deprecated annotation
        if (ctor.attrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        // Parameters
        new CallableGenerator(ctor).generateMethodParameters(builder);

        // Exception
        if (ctor.attrs().throws_())
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
        if (ctor.infoElements().doc() != null)
            builder.addJavadoc(new DocGenerator(ctor.infoElements().doc()).generate());

        // Deprecated annotation
        if (ctor.attrs().deprecated())
            builder.addAnnotation(Deprecated.class);

        // Parameters
        new CallableGenerator(ctor).generateMethodParameters(builder);

        // Exception
        if (ctor.attrs().throws_())
            builder.addException(ClassNames.GERROR_EXCEPTION);

        // Platform check
        if (ctor.doPlatformCheck())
            builder.addStatement("$T.checkSupportedPlatform($L)",
                    ClassNames.PLATFORM, Platform.toStringLiterals(ctor.platforms()));

        builder.addStatement("var _result = $L($L)", privateMethodName, parameterNames());

        // Marshal return value and handle ownership transfer
        var generator = new TypedValueGenerator(ctor.returnValue());
        PartialStatement stmt = generator.marshalNativeToJava("_result", false);
        if (parent.isGObject()) {
            builder.addNamedCode(PartialStatement.of("var _object = ").add(stmt).format() + ";\n", stmt.arguments())
                    .beginControlFlow("if (_object != null)")
                    .addStatement("$T.debug($S, _object == null || _object.handle() == null ? 0 : _object.handle())",
                            ClassNames.GLIB_LOGGER, "Ref " + parent.typeName() + " %ld\\n")
                    .addStatement("_object.ref()")
                    .endControlFlow()
                    .addStatement("return ($T) _object", parent.typeName());
        } else if (parent instanceof Record record) {
            builder.addNamedCode(PartialStatement.of("var _instance = ").add(stmt).add(";\n").format(), stmt.arguments())
                    .beginControlFlow("if (_instance != null)")
                    .addStatement("$T.takeOwnership(_instance.handle())", ClassNames.MEMORY_CLEANER);
            new RecordGenerator(record).setFreeFunc(builder, "_instance", parent.typeName());
            builder.endControlFlow()
                    .addStatement("return ($T) _instance", parent.typeName());
        } else {
            stmt = PartialStatement.of("return ($parentType:T) ", "parentType", parent.typeName()).add(stmt);
            builder.addNamedCode(stmt.format(), stmt.arguments());
        }
        return builder.build();
    }

    private String parameterNames() {
        if (ctor.parameters() == null) return "";
        return ctor.parameters().parameters().stream()
                .filter(not(Parameter::isUserDataParameter))
                .filter(not(Parameter::isDestroyNotifyParameter))
                .filter(not(Parameter::isArrayLengthParameter))
                .map(TypedValue::name)
                .map(name -> "...".equals(name) ? "varargs" : Conversions.toJavaIdentifier(name))
                .collect(Collectors.joining(", "));
    }
}
