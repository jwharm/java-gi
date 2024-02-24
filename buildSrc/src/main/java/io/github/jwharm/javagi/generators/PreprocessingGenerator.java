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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.util.PartialStatement;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;
import java.util.Objects;

import static io.github.jwharm.javagi.util.Conversions.*;

public class PreprocessingGenerator extends TypedValueGenerator {

    private final Parameter p;

    public PreprocessingGenerator(Parameter p) {
        super(p);
        this.p = p;
    }

    public void generate(MethodSpec.Builder builder) {
        nullCheck(builder);
        pointerAllocation(builder);
        arrayLength(builder);
        scope(builder);
    }

    public void generateUpcall(MethodSpec.Builder builder) {
        readPrimitiveAliasPointer(builder);
        readOutParameter(builder);
    }

    // Don't null-check parameters that are hidden from the Java API, or primitive values
    private void nullCheck(MethodSpec.Builder builder) {
        if (p.notNull() &&
                (! (p.isErrorParameter()
                    || p.isUserDataParameter()
                    || p.isDestroyNotifyParameter()
                    || p.isArrayLengthParameter()
                    || p.varargs()
                    || (type != null && type.isPrimitive() && !type.isPointer())))) {
            builder.addStatement("$T.requireNonNull($L, $S)",
                    Objects.class,
                    getName(),
                    "Parameter '" + getName() + "' must not be null");
        }
    }

    // Allocate memory for out-parameter
    private void pointerAllocation(MethodSpec.Builder builder) {
        if (p.isOutParameter()
                && array != null
                && (!array.unknownSize())) {
            /*
             * Out-parameter array with known size: If the array isn't null,
             * copy the contents into the native memory buffer. If it is null,
             * allocate a pointer.
             */
            var stmt = PartialStatement
                    .of("$memorySegment:T _$name:LPointer = ($name:L == null || $name:L.get() == null)$W? ")
                    .add("_arena.allocate($valueLayout:T.$layout:L)")
                    .add("$W: ")
                    .add("($memorySegment:T) ")
                    .add(marshalJavaToNative(getName() + ".get()"))
                    .add(";\n",
                            "memorySegment", MemorySegment.class,
                            "name", getName(),
                            "valueLayout", ValueLayout.class,
                            "layout", getValueLayoutPlain(type)
                    );
            builder.addNamedCode(stmt.format(), stmt.arguments());
        } else if (p.isOutParameter()
                || (type != null
                    && type.isPointer()
                    && target instanceof Alias a
                    && a.type().isPrimitive())) {
            builder.addStatement("$T _$LPointer = _arena.allocate($T.$L)",
                    MemorySegment.class,
                    getName(),
                    ValueLayout.class,
                    getValueLayoutPlain(type));
        }
    }

    // Declare a Java variable with the array length, so the length-parameter
    // can be omitted from the Java API
    private void arrayLength(MethodSpec.Builder builder) {
        if (p.isArrayLengthParameter()) {
            if (p.isOutParameter()) {
                // Set the initial value of the allocated pointer to the length of the input array
                if (p.isArrayLengthParameter() && p.isOutParameter()) {
                    var stmt = PartialStatement.of("_$name:LPointer.set($valueLayout:T.$layout:L, 0L,$W",
                                    "name", getName(),
                                    "valueLayout", ValueLayout.class,
                                    "layout", getValueLayoutPlain(type))
                            .add(arrayLengthStatement())
                            .add(");\n");
                    builder.addNamedCode(stmt.format(), stmt.arguments());
                }
                // Declare an Out<> instance
                builder.addStatement("$1T $2L = new $3T<>()",
                        getType(),
                        getName(),
                        ClassNames.OUT);
            } else {
                // Declare a primitive value
                var stmt = PartialStatement.of("$type:T $name:L =$W",
                                "type", getType(),
                                "name", getName())
                        .add(arrayLengthStatement())
                        .add(";\n");
                builder.addNamedCode(stmt.format(), stmt.arguments());
            }
        }
    }

    private PartialStatement arrayLengthStatement() {
        // Find the name of the array-parameter
        Parameter arrayParam = p.parent().parameters().stream()
                .filter(q -> q.anyType() instanceof Array a && a.length() == p)
                .findAny()
                .orElse(null);

        // Fallback to default value 0 (usually when the array is in the return value)
        if (arrayParam == null)
            return PartialStatement.of(literal(p.anyType().typeName(), "0"));

        return PartialStatement.of("$arr:L == null ? $zero:L : $cast:L",
                        "arr", toJavaIdentifier(arrayParam.name()),
                        "zero", literal(p.anyType().typeName(), "0"),
                        "cast", List.of("byte", "short").contains(type.javaType())
                                ? "(" + type.javaType() + ") "
                                : ""
                )
                .add(arrayParam.isOutParameter()
                        ? "$arr:L.get() == null ? $zero:L : $cast:L$arr:L.get().length"
                        : "$arr:L.length"
                );
    }

    // Arena for parameters with async or notified scope
    private void scope(MethodSpec.Builder builder) {
        if (p.scope() == Scope.NOTIFIED && p.destroy() != null)
            builder.addStatement("final $1T _$2LScope = $1T.ofConfined()",
                            Arena.class,
                            getName())
                    .addStatement("final $1T _$2LDestroyNotify = $$ -> _$2LScope.close()",
                            ClassName.get("org.gnome.glib", "DestroyNotify"),
                            getName());
        else if (p.scope() == Scope.ASYNC && (!p.isDestroyNotifyParameter()))
            builder.addStatement("final $1T _$2LScope = $1T.ofConfined()",
                            Arena.class,
                            getName())
                    .addStatement("if ($2L != null) $1T.CLEANER.register($2L, new $1T(_$2LScope))",
                            ClassNames.ARENA_CLOSE_ACTION,
                            getName());
    }

    // Read the value from a pointer to a primitive value and store it
    // in a Java Alias object
    private void readPrimitiveAliasPointer(MethodSpec.Builder builder) {
        if (target instanceof Alias a && a.type().isPrimitive() && type.isPointer()) {
            String layout = getValueLayoutPlain(type);
            builder.addStatement("$1T $2LParam = $2L.reinterpret($3T.$4L.byteSize(), _arena, null)",
                    MemorySegment.class,
                    getName(),
                    ValueLayout.class,
                    layout);
            builder.addStatement("$1T _$2LAlias = new $1T($2LParam.get($3T.$4L, 0))",
                    type.typeName(),
                    getName(),
                    ValueLayout.class,
                    layout);
        }
    }

    // Read the pre-existing value of an out-parameter and store it in
    // a Java Out<...> instance
    private void readOutParameter(MethodSpec.Builder builder) {
        if (!p.isOutParameter())
            return;

        // Pointer to a single value
        if (type != null) {
            String layout = getValueLayoutPlain(type);
            builder.addStatement("$1T $2LParam = $2L.reinterpret($3T.$4L.byteSize(), _arena, null)",
                    MemorySegment.class,
                    getName(),
                    ValueLayout.class,
                    layout);

            if (type.isPrimitive() || target instanceof Alias a && a.type().isPrimitive()) {
                builder.addStatement("$1T _$2LOut = new $3T<>($2LParam.get($4T.$5L, 0)$6L)",
                        getType(),
                        getName(),
                        ClassNames.OUT,
                        ValueLayout.class,
                        layout,
                        type.isBoolean() ? " != 0" : "");
            } else {
                String identifier = getName() + "Param";
                if (target instanceof FlaggedType)
                    identifier += ".get($valueLayout:T.JAVA_INT, 0)";

                var stmt = PartialStatement.of("$outType:T _" + getName() + "Out = new $out:T<>(",
                                "valueLayout", ValueLayout.class,
                                "outType", getType(),
                                "out", ClassNames.OUT)
                        .add(marshalNativeToJava(type, identifier, true))
                        .add(");\n");
                builder.addNamedCode(stmt.format(), stmt.arguments());
            }
        }

        // Pointer to an array
        else if (array != null) {
            var stmt = PartialStatement.of("$outType:T _" + getName() + "Out = new $out:T<>(",
                            "outType", getType(),
                            "out", ClassNames.OUT)
                    .add(marshalNativeToJava(getName(), true))
                    .add(");\n");
            builder.addNamedCode(stmt.format(), stmt.arguments());
        }
    }
}
