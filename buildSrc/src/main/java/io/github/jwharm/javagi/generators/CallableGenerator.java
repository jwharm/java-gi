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
import com.squareup.javapoet.ParameterSpec;
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.util.Conversions;
import io.github.jwharm.javagi.util.PartialStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class CallableGenerator {

    private final Callable callable;

    public CallableGenerator(Callable callable) {
        this.callable = callable;
    }

    void generateFunctionDescriptor(MethodSpec.Builder builder) {
        List<String> valueLayouts = new ArrayList<>();

        boolean isVoid = callable.returnValue().anyType() instanceof Type t && t.isVoid();
        if (!isVoid)
            valueLayouts.add(Conversions.getValueLayout(callable.returnValue().anyType()));

        if (callable instanceof Signal)
            valueLayouts.add("ADDRESS");

        if (callable.parameters() != null) {
            if (callable.parameters().instanceParameter() != null)
                valueLayouts.add("ADDRESS");
            valueLayouts.addAll(
                    callable.parameters().parameters().stream()
                            .filter(not(Parameter::varargs))
                            .map(Parameter::anyType)
                            .map(Conversions::getValueLayout)
                            .toList());
        }

        if (callable.throws_())
            valueLayouts.add("ADDRESS");

        if (valueLayouts.isEmpty()) {
            builder.addStatement("$1T _fdesc = $1T.ofVoid()", FunctionDescriptor.class);
        } else {
            String layouts = valueLayouts.stream()
                    .map(s -> "$2T." + s)
                    // $Z will split long lines
                    .collect(Collectors.joining(",$W", "(", ")"));
            builder.addStatement("$1T _fdesc = $1T.$3L" + layouts,
                    FunctionDescriptor.class, ValueLayout.class, isVoid ? "ofVoid" : "of");
        }
    }

    void generateMethodParameters(MethodSpec.Builder builder) {
        generateMethodParameters(builder, false);
    }

    void generateMethodParameters(MethodSpec.Builder builder, boolean generic) {
        if (callable.parameters() == null)
            return;

        for (var p : callable.parameters().parameters()) {
            if (p.isUserDataParameter() || p.isDestroyNotifyParameter() || p.isArrayLengthParameter())
                continue;

            if (p.varargs()) {
                builder.addParameter(Object[].class, "varargs");
                builder.varargs(true);
            } else {
                var generator = new TypedValueGenerator(p);
                var type = generator.getType();
                if (generic && type.equals(ClassNames.GOBJECT))
                    type = ClassNames.GENERIC_T;
                var spec = ParameterSpec.builder(type, generator.getName());
                if (p.nullable())
                    spec.addAnnotation(Nullable.class);
                else if (p.notNull())
                    spec.addAnnotation(NotNull.class);
                builder.addParameter(spec.build());
            }
        }
    }

    PartialStatement marshalParameters() {
        var parameters = callable.parameters();
        if (parameters == null)
            return callable.throws_() ? PartialStatement.of("_gerror") : new PartialStatement();

        PartialStatement stmt = new PartialStatement();

        if (parameters.instanceParameter() != null)
            stmt.add("handle()");

        for (Parameter p : parameters.parameters()) {
            if (!stmt.format().isEmpty()) stmt.add(", ");
            stmt.add("$Z"); // emit newline
            var generator = new TypedValueGenerator(p);

            // Generate null-check. But don't null-check parameters that are
            // hidden from the Java API, or primitive values
            if (generator.checkNull())
                stmt.add("($memorySegment:T) (" + generator.getName() + " == null ? $memorySegment:T.NULL : ",
                        "memorySegment", MemorySegment.class);

            // callback destroy
            if (p.isDestroyNotifyParameter()) {
                var notify = parameters.parameters().stream()
                        .filter(q -> q.destroy() == p)
                        .findAny();
                if (notify.isPresent()) {
                    String notifyName = Conversions.toJavaIdentifier(notify.get().name());
                    stmt.add("_" + notifyName + "DestroyNotify.toCallback(_" + notifyName + "Scope)");
                } else {
                    stmt.add("$memorySegment:T.NULL", "memorySegment", MemorySegment.class);
                }
            }

            // user_data
            else if (p.isUserDataParameter())
                stmt.add("$memorySegment:T.NULL", "memorySegment", MemorySegment.class);

            // Varargs
            else if (p.varargs())
                stmt.add("varargs");

            // Preprocessing statement
            else if (p.isOutParameter()
                    || (p.anyType() instanceof Type type
                        && type.get() instanceof Alias a
                        && a.type().isPrimitive()
                        && type.isPointer())) {
                stmt.add("_" + generator.getName() + "Pointer");
            }

            // Custom interop
            else
                stmt.add(generator.marshalJavaToNative(generator.getName()));

            // Closing parentheses for null-check
            if (generator.checkNull())
                stmt.add(")");
        }

        // GError
        if (callable.throws_())
            stmt.add(", _gerror");

        return stmt;
    }

    boolean varargs() {
        return callable.parameters() != null
                && callable.parameters().parameters().stream().anyMatch(Parameter::varargs);
    }
}
