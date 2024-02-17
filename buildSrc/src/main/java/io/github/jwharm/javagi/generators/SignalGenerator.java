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
import io.github.jwharm.javagi.util.PartialStatement;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Modifier;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Comparator;

import static io.github.jwharm.javagi.util.Conversions.getValueLayout;
import static io.github.jwharm.javagi.util.Conversions.toCamelCase;

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
                        ClassNames.SIGNAL_CONNECTION,
                        signal.typeName()
                ));

        if (signal.parent() instanceof Interface)
            builder.addModifiers(Modifier.DEFAULT);

        if (signal.infoElements().doc() != null)
            builder.addJavadoc(new DocGenerator(signal.infoElements().doc()).generate(true));

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

        return builder.addStatement("var _callback = handler.toCallback($T.global())",
                        Arena.class)
                .addStatement("var _result = (long) $1T.g_signal_connect_data.invokeExact(handle(), _name, _callback, $2T.NULL, $2T.NULL, 0)",
                        ClassNames.SIGNALS,
                        MemorySegment.class)
                .addStatement("return new SignalConnection<>(handle(), _result)")
                .nextControlFlow("catch (Throwable _err)")
                .addStatement("throw new AssertionError($S, _err)", "Unexpected exception occured: ")
                .endControlFlow()
                .endControlFlow()
                .build();
    }

    public boolean emitMethodExists() {
        String name = "emit_" + signal.name().replace("-", "_");
        return signal.parent().children().stream()
                .filter(node -> node instanceof Method || node instanceof Function)
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
        builder.returns(new TypedValueGenerator(signal.returnValue()).getType());

        // Add source parameter for signals
        if (signal.detailed())
            builder.addParameter(
                    ParameterSpec.builder(String.class, "detail")
                            .addAnnotation(Nullable.class)
                            .build());

        // Add method parameters
        generator.generateMethodParameters(builder);

        // Arena for memory allocations
        builder.beginControlFlow("try ($1T _arena = $1T.ofConfined())",
                Arena.class);

        // Parameter preprocessing
        if (signal.parameters() != null)
            signal.parameters().parameters().stream()
                    // Array parameters may refer to other parameters for their length,
                    // so they must be processed last.
                    .sorted((Comparator.comparing(p -> p.anyType() instanceof Array)))
                    .forEach(p -> new PreprocessingGenerator(p).generate(builder));

        // Allocate memory for return value
        if (!signal.returnValue().anyType().isVoid())
            builder.addStatement("$T _result = _arena.allocate($T.$L)",
                    MemorySegment.class,
                    ValueLayout.class,
                    getValueLayout(signal.returnValue().anyType()));

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
        if (signal.parameters() == null && signal.returnValue().anyType().isVoid()) {
            varargs.add("new Object[0]");
        }

        // Generate parameter marshaling for all parameters, to store into the array
        else {
            varargs.add("new Object[] {");
            if (signal.parameters() != null) {
                varargs.add(generator.marshalParameters());
            }
            if (!signal.returnValue().anyType().isVoid()) {
                if (! varargs.format().endsWith("{"))
                    varargs.add(", ");
                varargs.add("_result");
            }
            varargs.add("}");
        }
        builder.addNamedCode(varargs.format() + ";\n", varargs.arguments());

        // Emit the signal
        builder.addStatement("$T.g_signal_emit_by_name.invokeExact(handle(), _name, _arena, _args)",
                ClassNames.SIGNALS);

        // Parameter postprocessing
        if (signal.parameters() != null)
            for (var p : signal.parameters().parameters())
                new PostprocessingGenerator(p).generate(builder);

        // Marshal the return value
        if (!signal.returnValue().anyType().isVoid()) {
            var generator = new TypedValueGenerator(signal.returnValue());
            var layout = getValueLayout(signal.returnValue().anyType());
            var stmt = PartialStatement.of("return ", "valueLayout", ValueLayout.class);
            stmt.add(generator.marshalNativeToJava("_result.get($valueLayout:T." + layout + ", 0)", false));
            builder.addNamedCode(stmt.format() + ";\n", stmt.arguments());
        }

        // Log exceptions
        return builder.nextControlFlow("catch (Throwable _err)")
                .addStatement("throw new AssertionError($S, _err)",
                        "Unexpected exception occurred: ")
                .endControlFlow()
                .build();
    }
}
