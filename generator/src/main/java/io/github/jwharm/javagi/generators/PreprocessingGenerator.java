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

package io.github.jwharm.javagi.generators;

import com.squareup.javapoet.MethodSpec;
import io.github.jwharm.javagi.configuration.ClassNames;
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Record;
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
        transferOwnership(builder);
        createGBytes(builder);
    }

    public void generateUpcall(MethodSpec.Builder builder) {
        readPrimitiveAliasPointer(builder);
        readOutParameter(builder);
    }

    /*
     * Don't null-check parameters that are hidden from the Java API, or
     * primitive values.
     */
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
        if (p.isOutParameter() && array != null && !array.unknownSize()) {
            /*
             * Out-parameter array with known size: If the array isn't null,
             * copy the contents into the native memory buffer. If it is null,
             * allocate a pointer.
             */
            var stmt = PartialStatement.of(
                            "$memorySegment:T _$name:LPointer = ($name:L == null || $name:L.get() == null)$W? ",
                            "memorySegment", MemorySegment.class,
                            "name", getName())
                    .add("_arena.allocate(")
                    .add(generateValueLayoutPlain(type))
                    .add(")$W: ")
                    .add("($memorySegment:T) ")
                    .add(marshalJavaToNative(getName() + ".get()"))
                    .add(";\n");
            builder.addNamedCode(stmt.format(), stmt.arguments());
        } else if ((p.isOutParameter() && !p.isDestroyNotifyParameter())
                || (type != null
                    && type.isPointer()
                    && target instanceof Alias a
                    && a.isValueWrapper())) {
            PartialStatement stmt;
            if (p.isArrayLengthParameter() || p.isUserDataParameterForDestroyNotify()
                    || (target != null && target.checkIsGBytes())) {
                /*
                 * Allocate an empty memory segment with the correct layout
                 */
                stmt = PartialStatement.of(
                                "$memorySegment:T _$name:LPointer = _arena.allocate(",
                                "memorySegment", MemorySegment.class,
                                "name", getName())
                        .add(generateValueLayoutPlain(type))
                        .add(");\n");
            } else {
                /*
                 * Allocate a memory segment with the parameter's input value.
                 * We do this for both "inout" and "out" parameters, even
                 * though it should only be required for "inout".
                 */
                String identifier = getName();
                if (! (target instanceof Alias a && a.isValueWrapper()))
                        identifier = identifier + ".get()";

                identifier = "(" + getName() + " == null ? null : " + identifier + ")";

                // Handle an Out<Boolean> where the value is null.
                if (type != null && type.isBoolean())
                    identifier = "Boolean.TRUE.equals" + identifier;

                stmt = PartialStatement.of(
                                "$memorySegment:T _$name:LPointer = _arena.allocateFrom($Z",
                                "memorySegment", MemorySegment.class,
                                "name", getName())
                        .add(generateValueLayoutPlain(type))
                        .add(", ")
                        .add(marshalJavaToNative(identifier))
                        .add(");\n");
            }
            builder.addNamedCode(stmt.format(), stmt.arguments());
        }
    }

    /*
     * Declare a Java variable with the array length, so the length-parameter
     * can be omitted from the Java API.
     */
    private void arrayLength(MethodSpec.Builder builder) {
        if (!p.isArrayLengthParameter())
            return;

        if (p.isOutParameter()) {
            // Set the initial value of the allocated pointer to the length
            // of the input array
            if (p.isArrayLengthParameter() && p.isOutParameter()) {
                var stmt = PartialStatement.of("_$name:LPointer.set(", "name", getName())
                        .add(generateValueLayoutPlain(type))
                        .add(", 0L,$W")
                        .add(arrayLengthStatement())
                        .add(");\n");
                builder.addNamedCode(stmt.format(), stmt.arguments());
            }
            // Declare an Out<> instance
            builder.addStatement("$1T $2L = new $3T<>()",
                    getType(), getName(), ClassNames.OUT);
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
                                : "")
                .add(arrayParam.isOutParameter()
                        ? "$arr:L.get() == null ? $zero:L : $cast:L$arr:L.get().length"
                        : "$arr:L.length");
    }

    // Arena for parameters with async or notified scope
    private void scope(MethodSpec.Builder builder) {
        if (p.scope() == Scope.NOTIFIED && p.destroy() != null)
            builder.addStatement("final $1T _$2LScope = $1T.ofShared()",
                            Arena.class, getName());

        if (p.scope() == Scope.ASYNC && !p.isDestroyNotifyParameter())
            builder.addStatement("final $1T _$2LScope = $1T.ofShared()",
                            Arena.class, getName())
                   .addStatement("if ($2L != null) $1T.CLEANER.register($2L, new $1T(_$2LScope))",
                            ClassNames.ARENA_CLOSE_ACTION, getName());
    }

    // If the parameter has attribute transfer-ownership="full", we must
    // register a reference, because the native code is going to call unref()
    // at some point while we still keep a pointer in the InstanceCache.
    private void transferOwnership(MethodSpec.Builder builder) {
        // GObjects where ownership is fully transferred away (unless it's an
        // out parameter or a pointer)
        if (target != null && target.checkIsGObject()
                && p.transferOwnership() != TransferOwnership.NONE
                && !p.isOutParameter()
                && (type.cType() == null || !type.cType().endsWith("**"))) {
            builder.beginControlFlow("if ($L instanceof $T _gobject)",
                            getName(), ClassNames.G_OBJECT)
                    .addStatement("$T.debug($S, _gobject.handle().address())",
                            ClassNames.GLIB_LOGGER,
                            "Ref " + type.typeName() + " %ld")
                    .addStatement("_gobject.ref()")
                    .endControlFlow();
        }

        // Same, but for structs/unions: Disable the cleaner
        else if (target != null
                && !(target instanceof Alias a && a.isValueWrapper())
                && !target.checkIsGBytes()
                && p.transferOwnership() != TransferOwnership.NONE
                && !p.isOutParameter()
                && !(target instanceof Record r && r.foreign())
                && (type.cType() == null || !type.cType().endsWith("**"))) {
            builder.addStatement(
                    checkNull() ? "if ($1L != null) $2T.yieldOwnership($1L)"
                                : "$2T.yieldOwnership($1L)",
                    getName(),
                    ClassNames.MEMORY_CLEANER);
        }

        // Same, but for arrays
        else if (array != null
                && array.anyType() instanceof Type elemType
                && p.transferOwnership() != TransferOwnership.NONE
                && !p.isOutParameter()
                && elemType.lookup() != null) {
            var elemTarget = elemType.lookup();
            if (checkNull())
                builder.beginControlFlow("if ($L != null)", getName());
            builder.beginControlFlow("for (var _element : $L)", getName());
            if (elemTarget.checkIsGObject()) {
                builder.beginControlFlow("if (_element instanceof $T _gobject)",
                                ClassNames.G_OBJECT)
                        .addStatement("$T.debug($S, _gobject.handle().address())",
                                ClassNames.GLIB_LOGGER,
                                "Ref " + elemType.typeName() + " %ld")
                        .addStatement("_gobject.ref()")
                        .endControlFlow();
            } else if (! (target instanceof Alias a && a.isValueWrapper())) {
                builder.addStatement("if (_element != null) $T.yieldOwnership(_element)",
                                ClassNames.MEMORY_CLEANER);
            }
            builder.endControlFlow();
            if (checkNull())
                builder.endControlFlow();
        }
    }

    private void createGBytes(MethodSpec.Builder builder) {
        if (target != null && target.checkIsGBytes()) {
            if (p.isOutParameter()) {
                builder.addStatement("_$1LPointer.set($2T.ADDRESS, 0L, $3T.toGBytes($1L == null ? null : $1L.get()))",
                        getName(), ValueLayout.class, ClassNames.INTEROP);
            } else {
                builder.addStatement("$1T _$3LGBytes = $2T.toGBytes($3L)",
                        MemorySegment.class, ClassNames.INTEROP, getName());
            }
        }
    }

    // Read the value from a pointer to a primitive value and store it
    // in a Java Alias object
    private void readPrimitiveAliasPointer(MethodSpec.Builder builder) {
        if (target instanceof Alias a && a.isValueWrapper() && type.isPointer()) {
            var stmt = PartialStatement.of(
                            "$memorySegment:T $name:LParam = $name:L.reinterpret(",
                            "memorySegment", MemorySegment.class,
                            "name", getName())
                    .add(generateValueLayoutPlain(type))
                    .add(".byteSize(), _arena, null);\n");
            builder.addNamedCode(stmt.format(), stmt.arguments());

            stmt = PartialStatement.of(
                            "$aliasType:T _$name:LAlias = new $aliasType:T($name:LParam.get(",
                            "aliasType", type.typeName(),
                            "name", getName())
                    .add(generateValueLayoutPlain(type))
                    .add(", 0));\n");
            builder.addNamedCode(stmt.format(), stmt.arguments());
        }
    }

    // Read the pre-existing value of an out-parameter and store it in
    // a Java Out<...> instance
    private void readOutParameter(MethodSpec.Builder builder) {
        if (!p.isOutParameter())
            return;

        // Pointer to an array
        if (array != null) {
            var stmt = PartialStatement.of("$outType:T _" + getName() + "Out = new $out:T<>(",
                            "outType", getType(),
                            "out", ClassNames.OUT)
                    .add(marshalNativeToJava(getName(), true))
                    .add(");\n");
            builder.addNamedCode(stmt.format(), stmt.arguments());
            return;
        }

        if (type == null)
            return;

        // Pointer to a single value
        var stmt = PartialStatement.of(
                        "$memorySegment:T $name:LParam = $name:L.reinterpret(",
                        "memorySegment", MemorySegment.class,
                        "name", getName())
                .add(generateValueLayoutPlain(type))
                .add(".byteSize(), _arena, null);\n");
        builder.addNamedCode(stmt.format(), stmt.arguments());

        if (type.isPrimitive() || target instanceof Alias a && a.isValueWrapper()) {
            stmt = PartialStatement.of(
                            "$outType:T _$name:LOut = new $out:T<>($name:LParam.get(",
                            "outType", getType(),
                            "name", getName(),
                            "out", ClassNames.OUT)
                    .add(generateValueLayoutPlain(type))
                    .add(", 0)")
                    .add(type.isBoolean() ? " != 0" : "")
                    .add(");\n");
            builder.addNamedCode(stmt.format(), stmt.arguments());
        } else {
            String identifier = getName() + "Param";
            if (target instanceof EnumType)
                identifier += ".get($valueLayout:T.JAVA_INT, 0)";

            stmt = PartialStatement.of("$outType:T _" + getName() + "Out = new $out:T<>(",
                            "valueLayout", ValueLayout.class,
                            "outType", getType(),
                            "out", ClassNames.OUT)
                    .add(marshalNativeToJava(type, identifier))
                    .add(");\n");
            builder.addNamedCode(stmt.format(), stmt.arguments());
        }
    }
}
