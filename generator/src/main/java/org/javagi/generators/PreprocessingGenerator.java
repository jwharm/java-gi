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
import org.javagi.configuration.ClassNames;
import org.javagi.gir.*;
import org.javagi.util.PartialStatement;
import org.javagi.gir.Record;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;
import java.util.Objects;

import static org.javagi.util.Conversions.*;
import static org.javagi.util.Conversions.toJavaBaseType;

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
        refGObject(builder);
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
            PartialStatement stmt;

            AnyType elemType = array.anyType();

            /*
             * Inout-parameter array with known size: If the array isn't null,
             * copy the contents into the native memory buffer. If it is null,
             * allocate a pointer.
             */
            if (p.direction() == Direction.INOUT) {
                stmt = PartialStatement.of(
                                "$memorySegment:T _$name:LArray = ($name:L == null || $name:L.get() == null)$W? ",
                                "memorySegment", MemorySegment.class,
                                "name", getName())
                        .add("_arena.allocate(")
                        .add(getValueLayout(elemType))
                        .add(")$W: ")
                        .add("($memorySegment:T) ")
                        .add(marshalJavaToNative(getName() + ".get()"))
                        .add(";\n");
            }

            /*
             * Caller-allocated out-parameter array with known size: allocate
             * a buffer and zero-initialize it.
             */
            else if (p.callerAllocates()) {
                if ("GLib.Array".equals(array.name())) {
                    String elemSize = "" + array.anyType().allocatedSize(false);
                    if (array.anyType() instanceof Type t && t.isLong())
                        elemSize = "$interop:T.longAsInt() ? 4 : 8";
                    stmt = PartialStatement.of(
                            "$memorySegment:T _$name:LArray = $interop:T.newGArray($elemSize:L);\n",
                            "memorySegment", MemorySegment.class,
                            "name", getName(),
                            "interop", ClassNames.INTEROP,
                            "elemSize", elemSize);
                } else if ("GLib.PtrArray".equals(array.name())) {
                    stmt = PartialStatement.of(
                            "$memorySegment:T _$name:LArray = $interop:T.newGPtrArray();\n",
                            "memorySegment", MemorySegment.class,
                            "name", getName(),
                            "interop", ClassNames.INTEROP);
                } else {
                    stmt = PartialStatement.of(
                                    "$memorySegment:T _$name:LArray = _arena.allocate(",
                                    "memorySegment", MemorySegment.class,
                                    "name", getName())
                            .add(elemType instanceof Type t ? getMemoryLayout(t) : getValueLayout(elemType))
                            .add(", ")
                            .add(array.sizeExpression(false))
                            .add(").fill((byte) 0);\n");
                }
            }

            else {
                stmt = PartialStatement.of("$memorySegment:T _$name:LArray = $memorySegment:T.NULL;\n",
                        "memorySegment", MemorySegment.class,
                        "name", getName());
            }

            builder.addNamedCode(stmt.format(), stmt.arguments());

            /*
             * When the c-type ends with "**", there is an extra level of
             * indirection needed, so we allocate another pointer.
             * The same goes for GStrv*, because that's actually a char***.
             * In all other cases, we just refer to the already allocated
             * memory.
             */
            boolean allocatePointer = false;
            if (array.cType() != null) {
                if (array.cType().endsWith("**"))
                    allocatePointer = true;
                else if (array.cType().equals("GStrv*"))
                    allocatePointer = true;
            }

            if (allocatePointer) {
                stmt = PartialStatement.of("$memorySegment:T _$name:LPointer = " +
                                "_arena.allocateFrom($valueLayout:T.ADDRESS, _$name:LArray);\n",
                        "memorySegment", MemorySegment.class,
                        "valueLayout", ValueLayout.class,
                        "name", getName());
            } else {
                stmt = PartialStatement.of("$memorySegment:T _$name:LPointer = _$name:LArray;\n",
                        "memorySegment", MemorySegment.class,
                        "name", getName());
            }
            builder.addNamedCode(stmt.format(), stmt.arguments());
        }

        // Handle all other out-parameters & pointers to primitive values
        else if ((p.isOutParameter() && !p.isDestroyNotifyParameter())
                || (type != null
                    && type.isPointer()
                    && target instanceof Alias a
                    && a.isValueWrapper())) {

            // Special case for length, user_data, and GBytes parameters
            if (p.isArrayLengthParameter() || p.isUserDataParameterForDestroyNotify()
                    || (target != null && target.checkIsGBytes())) {
                /*
                 * Allocate an empty memory segment with the correct layout
                 */
                var stmt = PartialStatement.of(
                                "$memorySegment:T _$name:LPointer = _arena.allocate(",
                                "memorySegment", MemorySegment.class,
                                "name", getName())
                        .add(getMemoryLayout(type))
                        .add(");\n");
                builder.addNamedCode(stmt.format(), stmt.arguments());
            }

            /*
             * All other cases:
             * Allocate a memory segment with the parameter's input value.
             * We do this for both "inout" and "out" parameters, even
             * though it should only be required for "inout".
             */
            else {

                // Handle an Out<> parameter with a primitive type
                if (type != null && type.isPrimitive()) {
                    var identifier = "$interop:T.to$boxed:L($name:L)";
                    var stmt = PartialStatement.of(
                                    "$memorySegment:T _$name:LPointer = _arena.allocateFrom($Z",
                                    "memorySegment", MemorySegment.class,
                                    "name", getName())
                            .add(getMemoryLayout(type))
                            .add(", ")
                            .add(marshalJavaToNative(identifier))
                            .add(");\n",
                                "interop", ClassNames.INTEROP,
                                "boxed", primitiveClassName(toJavaBaseType(type.name())),
                                "name", getName());
                    builder.addNamedCode(stmt.format(), stmt.arguments());
                }

                // Other Out<> parameters
                else {
                    String identifier = getName();
                    String nullCheck = identifier + " != null";
                    if (! (target instanceof Alias a && a.isValueWrapper())) {
                        identifier += ".get()";
                        nullCheck += " && " + identifier + " != null";
                    }

                    var valueLayout = getMemoryLayout(type);

                    var stmt = PartialStatement.of(
                                    "$memorySegment:T _$name:LPointer = _arena.allocate($Z",
                                    "memorySegment", MemorySegment.class,
                                    "name", getName())
                            .add(valueLayout)
                            .add(");\n");
                    builder.addNamedCode(stmt.format(), stmt.arguments());

                    // For inout parameters, when the value is not null, write it into the
                    // allocated memory segment.
                    if (p.direction() == Direction.INOUT) {
                        if (target != null
                                && (target.checkIsGList() || target.checkIsGHashTable() || target.checkIsGValue())) {
                            stmt = PartialStatement.of("_$name:LPointer.set($Z", "name", getName())
                                    .add("$valueLayout:T.ADDRESS", "valueLayout", ValueLayout.class)
                                    .add(", 0, ")
                                    .add(marshalJavaToNative(identifier))
                                    .add(");\n");
                        } else if (valueLayout.format().endsWith(".getMemoryLayout()")) {
                            stmt = PartialStatement.of("$interop:T.copy(")
                                    .add(marshalJavaToNative(identifier))
                                    .add(", _$name:LPointer, ")
                                    .add(valueLayout)
                                    .add(".byteSize());\n",
                                            "interop", ClassNames.INTEROP,
                                            "name", getName()
                                    );
                        } else {
                            stmt = PartialStatement.of("_$name:LPointer.set($Z", "name", getName())
                                    .add(valueLayout)
                                    .add(", 0, ")
                                    .add(marshalJavaToNative(identifier))
                                    .add(");\n");
                        }

                        builder.beginControlFlow("if (" + nullCheck + ")")
                                .addNamedCode(stmt.format(), stmt.arguments())
                                .endControlFlow();
                    }
                }
            }
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
                        .add(getValueLayout(type))
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

        // Transfer ownership of GList/GSList
        else if (target != null && target.checkIsGList() && !type.isActuallyAnArray()) {
            /* When full ownership is transferred to native code, our own code has no ownership anymore.
             * When no ownership is transferred, it's the reverse (we keep full ownership).
             * When native code becomes owner of the container, we keep ownership of the values.
             */
            String myOwnership = switch(p.transferOwnership()) {
                case FULL -> "NONE";
                case NONE -> "FULL";
                case CONTAINER -> "VALUES";
            };
            if (p.isOutParameter()) {
                builder.beginControlFlow("if ($1L != null && $1L.get() != null)",
                        getName());
                builder.addStatement("$L.get().setOwnership($T.$L)",
                        getName(), ClassNames.TRANSFER_OWNERSHIP, myOwnership);
            } else {
                builder.beginControlFlow("if ($1L != null)",
                        getName());
                builder.addStatement("$L.setOwnership($T.$L)",
                        getName(), ClassNames.TRANSFER_OWNERSHIP, myOwnership);
            }
            builder.endControlFlow();
        }

        // Transfer ownership of structs/unions: Disable the cleaner
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

        // Transfer ownership of arrays
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
                    .add(getValueLayout(type))
                    .add(".byteSize(), _arena, null);\n");
            builder.addNamedCode(stmt.format(), stmt.arguments());

            stmt = PartialStatement.of(
                            "$aliasType:T _$name:LAlias = new $aliasType:T($name:LParam.get(",
                            "aliasType", type.typeName(),
                            "name", getName())
                    .add(getValueLayout(type))
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
                .add(getValueLayout(type))
                .add(".byteSize(), _arena, null);\n");
        builder.addNamedCode(stmt.format(), stmt.arguments());

        if (type.isPrimitive() || target instanceof Alias a && a.isValueWrapper()) {
            stmt = PartialStatement.of(
                            "$outType:T _$name:LOut = new $out:T<>($name:LParam.get(",
                            "outType", getType(),
                            "name", getName(),
                            "out", ClassNames.OUT)
                    .add(getValueLayout(type))
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

    // Ref GObject when ownership is not transferred
    private void refGObject(MethodSpec.Builder builder) {
        if (p.transferOwnership() == TransferOwnership.NONE
                && target != null && target.checkIsGObject()) {
            var stmt = PartialStatement.of("var _" + getName() + "Cached = ")
                    .add(marshalNativeToJava(type, getName()))
                    .add(";\n");
            builder.addNamedCode(stmt.format(), stmt.arguments())
                    .beginControlFlow("if (_" + getName() + "Cached instanceof $T _gobject)", ClassNames.G_OBJECT)
                    .addStatement("_gobject.ref()")
                    .endControlFlow();
        }
    }
}
