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
import org.javagi.util.PartialStatement;
import org.javagi.gir.*;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Modifier;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static org.javagi.util.Conversions.toCamelCase;
import static java.util.Comparator.comparing;

public class SignalGenerator {

    private final Signal signal;
    private final String connectMethod;
    private final String emitMethod;
    private final CallableGenerator generator;

    public SignalGenerator(Signal signal) {
        this.signal = signal;
        this.connectMethod = "on" + toCamelCase(signal.name(), true);
        this.emitMethod = "emit" + toCamelCase(signal.name(), true);
        this.generator = new CallableGenerator(signal);
    }

    public TypeSpec generateFunctionalInterface() {
        return new ClosureGenerator(signal).generateFunctionalInterface();
    }

    public MethodSpec generateConnectMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(connectMethod)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(
                        ClassNames.SIGNAL_CONNECTION, signal.typeName()));

        if (signal.parent() instanceof Interface)
            builder.addModifiers(Modifier.DEFAULT);

        var doc = signal.infoElements().doc();
        if (doc != null)
            builder.addJavadoc(new DocGenerator(doc).generate(true));
        builder.addJavadoc("@see $LCallback#run\n",
                toCamelCase(signal.name(), true));

        if (signal.deprecated())
            builder.addAnnotation(Deprecated.class);

        if (signal.detailed())
            builder.addParameter(
                    ParameterSpec.builder(String.class, "detail")
                            .addAnnotation(Nullable.class)
                            .build());

        builder.addParameter(signal.typeName(), "handler")
                .beginControlFlow("try ($1T _arena = $1T.ofConfined())",
                        Arena.class)
                .beginControlFlow("try");

        if (signal.detailed())
            builder.addStatement("var _name = $T.allocateNativeString($S + ((detail == null || detail.isBlank()) ? $S : ($S + detail)), _arena)",
                    ClassNames.INTEROP,
                    signal.name(),
                    "",
                    "::");
        else
            builder.addStatement("var _name = $T.allocateNativeString($S, _arena)",
                    ClassNames.INTEROP,
                    signal.name());

        return builder.addStatement("var _callbackArena = $T.ofShared()",
                        Arena.class)
                .addStatement("var _result = (int) (long) $1T.g_signal_connect_data.invokeExact("
                                + "$Zhandle(),"
                                + "$W_name,$Whandler.toCallback(_callbackArena),"
                                + "$W$2T.cacheArena(_callbackArena),"
                                + "$W$2T.CLOSE_CB_SYM,"
                                + "$W0)",
                        ClassNames.SIGNALS,
                        ClassNames.ARENAS)
                .addStatement("return new SignalConnection<>(handle(), _result)")
                .nextControlFlow("catch (Throwable _err)")
                .addStatement("throw new AssertionError(_err)")
                .endControlFlow()
                .endControlFlow()
                .build();
    }

    public boolean emitMethodExists() {
        String name = "emit_" + signal.name().replace("-", "_");
        return signal.parent().children().stream()
                .filter(n -> n instanceof Method || n instanceof Function)
                .map(Callable.class::cast)
                .anyMatch(node -> name.equals(node.name()));
    }

    public MethodSpec generateEmitMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(emitMethod)
                .addModifiers(Modifier.PUBLIC);

        if (signal.parent() instanceof Interface)
            builder.addModifiers(Modifier.DEFAULT);

        // Javadoc
        if (signal.infoElements().doc() != null)
            builder.addJavadoc("Emits the $S signal. See {@link #$L}.",
                    signal.name(),
                    connectMethod);

        // Deprecated annotation
        if (signal.deprecated())
            builder.addAnnotation(Deprecated.class);

        // Return type
        ReturnValue returnValue = signal.returnValue();
        builder.returns(new TypedValueGenerator(returnValue).getType());

        // Add source parameter for signals
        if (signal.detailed())
            builder.addParameter(
                    ParameterSpec.builder(String.class, "detail")
                            .addAnnotation(Nullable.class)
                            .build());

        // Add method parameters
        generator.generateMethodParameters(builder, false, true);

        // Arena for memory allocations
        builder.beginControlFlow("try ($1T _arena = $1T.ofConfined())",
                Arena.class);

        // Parameter preprocessing
        if (signal.parameters() != null)
            signal.parameters().parameters().stream()
                    // Array parameters may refer to other parameters for their
                    // length, so they must be processed last.
                    .sorted((comparing(p -> p.anyType() instanceof Array)))
                    .map(PreprocessingGenerator::new)
                    .forEach(p -> p.generate(builder));

        // Determine ValueLayout for return value
        var returnLayout = PartialStatement.of(
                generator.generateValueLayout(returnValue.anyType()),
                "interop", ClassNames.INTEROP,
                "valueLayout", ValueLayout.class);

        // Allocate memory for return value
        if (!returnValue.anyType().isVoid()) {
            var stmt = PartialStatement.of("$[$memorySegment:T _result = _arena.allocate(",
                            "memorySegment", MemorySegment.class)
                    .add(returnLayout)
                    .add(");\n$]");
            builder.addNamedCode(stmt.format(), stmt.arguments());
        }

        // Allocate memory for signal name
        if (signal.detailed())
            builder.addStatement("$T _name = $T.allocateNativeString($S + ((detail == null || detail.isBlank()) ? $S : ($S + detail)), _arena)",
                    MemorySegment.class,
                    ClassNames.INTEROP,
                    signal.name(),
                    "",
                    "::");
        else
            builder.addStatement("$T _name = $T.allocateNativeString($S, _arena)",
                    MemorySegment.class,
                    ClassNames.INTEROP,
                    signal.name());

        // Create an array with the signal arguments
        PartialStatement varargs = PartialStatement.of("Object[] _args = ");

        // Empty array when there are no parameters
        if (signal.parameters() == null && returnValue.anyType().isVoid()) {
            varargs.add("new Object[0]");
        }

        // Generate parameter marshaling for all parameters, to store into the
        // array
        else {
            varargs.add("new Object[] {");
            if (signal.parameters() != null) {
                varargs.add(generator.marshalParameters(true));
            }
            if (!returnValue.anyType().isVoid()) {
                if (! varargs.format().endsWith("{"))
                    varargs.add(", ");
                varargs.add("_result");
            }
            varargs.add("}");
        }
        builder.addNamedCode(varargs.format() + ";\n", varargs.arguments());

        // Emit the signal
        builder.addStatement("$T.g_signal_emit_by_name.invokeExact(handle(), _name, _args)",
                ClassNames.SIGNALS);

        // Parameter postprocessing
        if (signal.parameters() != null)
            for (var p : signal.parameters().parameters())
                new PostprocessingGenerator(p).generate(builder);

        // Marshal the return value
        if (!returnValue.anyType().isVoid()) {
            var generator = new TypedValueGenerator(returnValue);
            var identifier = "_result.get(" + returnLayout.format() + ", 0)";
            var stmt = PartialStatement.of("return ",
                            "valueLayout", ValueLayout.class)
                    .add(generator.marshalNativeToJava(identifier, false))
                    .add(";\n");
            stmt.arguments().putAll(returnLayout.arguments());
            builder.addNamedCode(stmt.format(), stmt.arguments());
        }

        // Log exceptions
        return builder.nextControlFlow("catch (Throwable _err)")
                .addStatement("throw new AssertionError(_err)")
                .endControlFlow()
                .build();
    }
}
