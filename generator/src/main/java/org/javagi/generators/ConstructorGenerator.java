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

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import org.javagi.configuration.ClassNames;
import org.javagi.gir.*;
import org.javagi.util.PartialStatement;
import org.javagi.util.Platform;
import org.javagi.gir.Class;
import org.javagi.gir.Record;

import javax.lang.model.element.Modifier;

import java.util.Collections;
import java.util.List;

import static org.javagi.util.Conversions.toJavaIdentifier;
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
                    && (! (parent instanceof Class c && c.abstract_()))
                ? constructor()
                : namedConstructor();

        MethodSpec helperMethod = new MethodGenerator(ctor, privateMethodName)
                .generate();

        return List.of(constructor, helperMethod);
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

        // These constructors return floating references
        else if (parent instanceof Record rec
                && List.of("GVariant", "GClosure").contains(rec.cType())) {
            builder.addStatement(rec.cType().equals("GVariant")
                            ? "refSink()" : "ref().sink()")
                    .addStatement("$T.takeOwnership(this)",
                            ClassNames.MEMORY_CLEANER)
                    .addStatement("$T.setFreeFunc(this, $S)",
                            ClassNames.MEMORY_CLEANER,
                            rec.attr("free-function"));
        }

        // Add cleaner to struct/union pointer
        else {
            builder.addStatement("$T.takeOwnership(this)",
                            ClassNames.MEMORY_CLEANER);
            new RegisteredTypeGenerator(parent).setFreeFunc(
                    builder, "this", parent.typeName());
        }

        return builder.build();
    }

    private MethodSpec namedConstructor() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        TypeName returnType = ctor.returnValue().anyType().typeName();
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
            builder.addStatement(Platform.generateSupportCheck(ctor.platforms()));

        builder.addStatement("var _result = $L($L)",
                privateMethodName,
                javaParameterNames());

        // Marshal return value and handle ownership transfer
        var generator = new TypedValueGenerator(ctor.returnValue());
        PartialStatement stmt = generator.marshalNativeToJava("_result", false);

        // Ref GObject
        if (parent.checkIsGObject()) {
            builder.addNamedCode(PartialStatement.of("return ")
                    .add(stmt).add(";\n").format(),stmt.arguments());
        }

        // GVariant constructors return floating references
        else if (parent instanceof Record rec
                && List.of("GVariant", "GClosure").contains(rec.cType())) {
            builder.addNamedCode(PartialStatement.of("var _instance = ")
                                    .add(stmt)
                                    .add(";\n")
                                    .format(),
                            stmt.arguments())
                    .beginControlFlow("if (_instance != null)")
                    .addStatement(rec.cType().equals("GVariant")
                            ? "_instance.refSink()" : "_instance.ref().sink()")
                    .addStatement("$T.takeOwnership(_instance)",
                            ClassNames.MEMORY_CLEANER)
                    .addStatement("$T.setFreeFunc(_instance, $S)",
                            ClassNames.MEMORY_CLEANER,
                            rec.attr("free-function"))
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
}
