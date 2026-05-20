/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2026 the Java-GI developers
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

import org.javagi.javapoet.CodeBlock;
import org.javagi.javapoet.MethodSpec;
import org.javagi.configuration.ClassNames;
import org.javagi.gir.*;
import org.javagi.gir.Record;

import java.lang.Class;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;
import java.util.Objects;

import static org.javagi.util.Conversions.*;
import static org.javagi.util.Conversions.toJavaBaseType;
import static org.javagi.util.Conversions.toJavaIdentifier;

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
        arraySizeCheck(builder);
        scope(builder);
        transferOwnership(builder);
        createGBytes(builder);
        createGString(builder);
    }

    public void generateUpcall(MethodSpec.Builder builder, boolean longAsInt) {
        readPrimitiveAliasPointer(builder);
        readOutParameter(builder, longAsInt);
        refGObject(builder);
        refGObjectUpcall(builder);
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
                    Objects.class, getName(), "Parameter '" + getName() + "' must not be null");
        }
    }

    // Allocate memory for out-parameter
    private void pointerAllocation(MethodSpec.Builder builder) {
        if (p.isOutParameter() && array != null && !array.unknownSize()) {
            AnyType elemType = array.anyType();

            /*
             * Inout-parameter array with known size: If the array isn't null,
             * copy the contents into the native memory buffer. If it is null,
             * allocate a pointer.
             */
            if (p.direction() == Direction.INOUT) {
                builder.addStatement("$1T _$2LArray = ($2L == null || $2L.get() == null)$W? _arena.allocate($3L)$W: ($1T) $4L",
                        MemorySegment.class,
                        getName(),
                        getValueLayout(elemType),
                        marshalJavaToNative(CodeBlock.of("$L.get()", getName())));
            }

            /*
             * Caller-allocated out-parameter array with known size: allocate
             * a buffer and zero-initialize it.
             */
            else if (p.callerAllocates()) {

                // Allocate new GArray
                if ("GLib.Array".equals(array.name())) {
                    CodeBlock elemSize = CodeBlock.of("$L", array.anyType().allocatedSize(false));
                    if (array.anyType() instanceof Type t && t.isLong())
                        elemSize = CodeBlock.of("$T.longAsInt() ? 4 : 8", ClassNames.INTEROP);
                    builder.addStatement("$T _$LArray = $T.newGArray($L)",
                            MemorySegment.class, getName(), ClassNames.INTEROP, elemSize);
                }

                // Allocate new GPtrArray
                else if ("GLib.PtrArray".equals(array.name())) {
                    builder.addStatement("$T _$LArray = $T.newGPtrArray()",
                            MemorySegment.class, getName(), ClassNames.INTEROP);
                }

                // Allocate new GByteArray
                else if ("GLib.ByteArray".equals(array.name())) {
                    builder.addStatement("$T _$LArray = $T.newGByteArray()",
                            MemorySegment.class, getName(), ClassNames.INTEROP);
                }

                // Allocate new regular array
                else {
                    builder.addStatement("$T _$LArray = $T.ofAuto().allocate($L, $L).fill((byte) 0)",
                            MemorySegment.class,
                            getName(),
                            Arena.class,
                            elemType instanceof Type t ? getMemoryLayout(t) : getValueLayout(elemType),
                            array.sizeExpression(false));
                }
            }

            else {
                builder.addStatement("$1T _$2LArray = $1T.NULL", MemorySegment.class, getName());
            }

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

            // Allocate pointer "_fooPointer" that points to "_fooArray"
            if (allocatePointer) {
                builder.addStatement("$1T _$2LPointer = _arena.allocateFrom($3T.ADDRESS, _$2LArray)",
                        MemorySegment.class, getName(), ValueLayout.class);
            }

            // When no pointer was allocated, "_fooPointer" is equal to "_fooArray"
            else {
                builder.addStatement("$1T _$2LPointer = _$2LArray",
                        MemorySegment.class, getName());
            }
        }

        // Handle all other out-parameters & pointers to primitive values
        else if (p.isOutParameter() && !p.isDestroyNotifyParameter()) {

            // Special case for length, user_data, GBytes and GString parameters
            if (p.isArrayLengthParameter() || p.isUserDataParameterForDestroyNotify()
                    || (target != null && (target.checkIsGBytes() || target.checkIsGString()))) {

                // Allocate an empty memory segment with the correct layout
                builder.addStatement("$T _$LPointer = _arena.allocate($L)",
                        MemorySegment.class, getName(), getMemoryLayout(type));
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
                    if (type.isLong()) {
                        // Long / unsigned long
                        builder.addStatement("$T _$LPointer", MemorySegment.class, getName())
                                .beginControlFlow("if ($T.longAsInt())", ClassNames.INTEROP)
                                .addStatement("_$1LPointer = _arena.allocateFrom($2T.JAVA_INT, $3T.toInteger($1L))",
                                        getName(), ValueLayout.class, ClassNames.INTEROP)
                                .nextControlFlow("else")
                                .addStatement("_$1LPointer = _arena.allocateFrom($2T.JAVA_LONG, (long) $3T.toInteger($1L))",
                                        getName(), ValueLayout.class, ClassNames.INTEROP)
                                .endControlFlow();
                    } else {
                        // All other primitive types
                        var identifier = CodeBlock.of("$T.to$L($L)",
                                ClassNames.INTEROP, primitiveClassName(toJavaBaseType(type.name())), getName());
                        builder.addStatement("$T _$LPointer = _arena.allocateFrom($Z$L, $L)",
                                MemorySegment.class, getName(), getMemoryLayout(type), marshalJavaToNative(identifier));
                    }
                }

                // Other Out<> parameters
                else {
                    var identifier = getName();
                    String nullCheck = identifier + " != null";
                    if (! (target instanceof Alias a && a.isValueWrapper())) {
                        identifier += ".get()";
                        nullCheck += " && " + identifier + " != null";
                    }
                    CodeBlock valueLayout = getMemoryLayout(type);

                    builder.addStatement("$T _$LPointer = _arena.allocate($Z$L)",
                            MemorySegment.class, getName(), valueLayout);

                    // For inout parameters, when the value is not null, write it into the
                    // allocated memory segment.
                    if (p.direction() == Direction.INOUT) {
                        builder.beginControlFlow("if (" + nullCheck + ")");
                        Callable copyFunc = target instanceof StandardLayoutType slt ? slt.copyFunction() : null;

                        // Marshal GList, GHashTable, GValue and GClosure
                        if (target != null
                                && (target.checkIsGList() || target.checkIsGHashTable() || target.checkIsGValue() || target.checkIsGClosure())) {
                            builder.addStatement("_$LPointer.set($Z$T.ADDRESS, 0, $L)",
                                    getName(),
                                    ValueLayout.class,
                                    marshalJavaToNative(CodeBlock.of(identifier)));
                        }

                        // Boxed copy
                        else if (target != null
                                && copyFunc == null
                                && valueLayout.toString().endsWith(".getMemoryLayout()")) {
                            builder.addStatement("_$1LPointer.set($2T.ADDRESS, 0, $3T.copy($4T.getType(), $1L.get(), "
                                            + "$5T.getMemoryLayout().byteSize()))",
                                    getName(),
                                    ValueLayout.class,
                                    ClassNames.BOXED_UTIL,
                                    target.typeName(),
                                    v.anyType().typeName());
                        }

                        // Call the copy-function
                        else if (copyFunc != null) {
                            builder.addStatement("_$1LPointer.set($2T.ADDRESS, 0, $1L.get().$3L().handle())",
                                    getName(), ValueLayout.class, toJavaIdentifier(copyFunc.name()));
                        }

                        // Run regular marshaling code
                        else {
                            builder.addStatement("_$LPointer.set($Z$L, 0, $L)",
                                    getName(), valueLayout, marshalJavaToNative(CodeBlock.of(identifier)));
                        }

                        builder.endControlFlow();
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
            if (p.isArrayLengthParameter() && p.isOutParameter())
                builder.addStatement("_$LPointer.set($L, 0L,$W$L)",
                        getName(), getValueLayout(type), arrayLengthStatement());
            // Declare an Out<> instance
            builder.addStatement("$1T $2L = new $3T<>()",
                    getType(), getName(), ClassNames.OUT);
        } else {
            // Declare a primitive value
            builder.addStatement("$T $L =$W$L",
                    getType(), getName(), arrayLengthStatement());
        }
    }

    private CodeBlock arrayLengthStatement() {
        String literal = literal(p.anyType().typeName(), "0");
        if (p.anyType() instanceof Type t && t.isLong())
            literal = "0";

        // Find the name of the array-parameter
        Parameter arrayParam = p.parent().parameters().stream()
                .filter(q -> q.anyType() instanceof Array a && a.length() == p)
                .findAny()
                .orElse(null);

        // Fallback to default value 0 (usually when the array is in the return value)
        if (arrayParam == null)
            return CodeBlock.of(literal);

        if (arrayParam.isOutParameter())
            return CodeBlock.of("$1L == null ? $2L : $3L$1L.get() == null ? $2L : $3L$1L.get().length",
                    toJavaIdentifier(arrayParam.name()),
                    literal,
                    List.of("byte", "short").contains(type.javaType()) ? "(" + type.javaType() + ") " : "");
        else
            return CodeBlock.of("$1L == null ? $2L : $3L$1L.length",
                    toJavaIdentifier(arrayParam.name()),
                    literal,
                    List.of("byte", "short").contains(type.javaType()) ? "(" + type.javaType() + ") " : "");
    }

    private void arraySizeCheck(MethodSpec.Builder builder) {
        if (!p.isOutParameter()
                && array != null
                && array.fixedSize() != -1) {
            boolean checkNull = checkNull();
            String name = getName();
            String size = "" + array.fixedSize();
            Class<?> error = IllegalArgumentException.class;

            if (checkNull)
                builder.beginControlFlow("if ($L != null)", name);

            builder.beginControlFlow("if ($L.length < $L)", name, size)
                    .addStatement("throw new $T($S)", error, name + ".length is less than " + size)
                    .endControlFlow()
                    .addJavadoc("@throws $T when length of {@code $L} is less than $L\n", error, name, size);

            if (checkNull)
                builder.endControlFlow();
        }
    }

    // Arena for parameters with async or notified scope
    private void scope(MethodSpec.Builder builder) {
        if (p.scope() == Scope.NOTIFIED && p.destroy() != null)
            builder.addStatement("final $1T _$2LScope = $1T.ofShared()", Arena.class, getName());

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
        // out parameter)
        if (target != null && target.checkIsGObject()
                && p.transferOwnership() != TransferOwnership.NONE
                && p.direction() != Direction.OUT) {
            String identifier = getName();
            if (p.direction() == Direction.INOUT && !type.isProxy())
                identifier = identifier + " != null && " + identifier + ".get()";

            builder.beginControlFlow("if ($L instanceof $T _gobject)", identifier, ClassNames.G_OBJECT)
                   .addStatement("_gobject.ref()")
                   .endControlFlow();
        }

        // Transfer ownership of GList/GSList
        else if (target != null && target.checkIsGList()
                && !type.isActuallyAnArray()
                && p.transferOwnership() != TransferOwnership.NONE) {
            // When full ownership is transferred to native code, our own code has no ownership anymore.
            // When native code becomes owner of the container, we keep ownership of the values.
            String owned = p.transferOwnership() == TransferOwnership.FULL ? "NONE" : "VALUES";

            if (p.isOutParameter()) {
                builder.beginControlFlow("if ($1L != null && $1L.get() != null)", getName())
                       .addStatement("$L.get().setOwnership($T.$L)", getName(), ClassNames.TRANSFER_OWNERSHIP, owned)
                       .endControlFlow();
            } else {
                builder.beginControlFlow("if ($1L != null)", getName())
                       .addStatement("$L.setOwnership($T.$L)", getName(), ClassNames.TRANSFER_OWNERSHIP, owned)
                       .endControlFlow();
            }
        }

        // Transfer ownership of structs/unions: Disable the cleaner
        else if (target != null
                && !(target instanceof Alias a && a.isValueWrapper())
                && !(target instanceof EnumType)
                && !target.checkIsGBytes()
                && !target.checkIsGString()
                && p.transferOwnership() != TransferOwnership.NONE
                && (p.direction() != Direction.OUT)
                && !(target instanceof Record r && r.foreign())) {

            if (checkNull())
                builder.beginControlFlow("if ($1L != null)", getName());

            var identifier = getName();
            if (p.isOutParameter())
                identifier += ".get()";

            builder.addStatement("$T.yieldOwnership($L)", ClassNames.MEMORY_CLEANER, identifier);

            if (checkNull())
                builder.endControlFlow();
        }

        // Yield ownership of array elements
        else if (array != null
                && array.anyType() instanceof Type elemType
                && p.transferOwnership() == TransferOwnership.FULL
                && p.direction() != Direction.OUT) {
            var elemTarget = elemType.lookup();

            // Check that the array elements can be owned
            if (elemTarget != null
                    && !(elemTarget instanceof Alias a && a.isValueWrapper())
                    && !(elemTarget instanceof EnumType)
                    && !elemTarget.checkIsGBytes()
                    && !elemTarget.checkIsGString()
                    && !(elemTarget instanceof Record r && r.foreign())) {

                // Check null
                if (checkNull())
                    builder.beginControlFlow("if ($L != null)", getName());

                // Get name (or name.get() for inout parameter)
                var identifier = getName();
                if (p.isOutParameter())
                    identifier += ".get()";

                // Loop through the array elements
                builder.beginControlFlow("for (var _element : $L)", identifier);

                // Yield ownership of each array element
                if (elemTarget.checkIsGObject()) {
                    builder.beginControlFlow("if (_element instanceof $T _gobject)", ClassNames.G_OBJECT)
                           .addStatement("_gobject.ref()")
                           .endControlFlow();
                } else {
                    builder.addStatement(
                            checkNull() ? "if (_element != null) $1T.yieldOwnership(_element)"
                                        : "$1T.yieldOwnership(_element)",
                            ClassNames.MEMORY_CLEANER);
                }

                // End of loop
                builder.endControlFlow();

                // End of null-check
                if (checkNull())
                    builder.endControlFlow();
            }
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

    private void createGString(MethodSpec.Builder builder) {
        if (target != null && target.checkIsGString()) {
            if (p.isOutParameter()) {
                builder.addStatement("_$1LPointer.set($2T.ADDRESS, 0L, $3T.toGString($1L == null ? null : $1L.get()))",
                        getName(), ValueLayout.class, ClassNames.INTEROP);
            } else {
                builder.addStatement("$1T _$3LGString = $2T.toGString($3L)",
                        MemorySegment.class, ClassNames.INTEROP, getName());
            }
        }
    }

    // Read the value from a pointer to a primitive value and store it
    // in a Java Alias object
    private void readPrimitiveAliasPointer(MethodSpec.Builder builder) {
        if (target instanceof Alias a && a.isValueWrapper()
                && type.isPointer()
                && !type.isUnannotatedReference()) {
            builder.addStatement("$1T $2LParam = $2L.reinterpret($3L.byteSize(), _arena, null)",
                        MemorySegment.class, getName(), getValueLayout(type))
                   .addStatement("$1T _$2LAlias = new $1T($2LParam.get($3L, 0))",
                        type.typeName(), getName(), getValueLayout(type));
        }
    }

    // Read the pre-existing value of an out-parameter and store it in
    // a Java Out<...> instance
    private void readOutParameter(MethodSpec.Builder builder, boolean longAsInt) {
        if (!p.isOutParameter())
            return;

        // Pointer to an array
        if (array != null) {
            builder.addStatement("$T _$LOut = new $T<>($L)",
                    getType(), getName(), ClassNames.OUT, marshalNativeToJava(CodeBlock.of(getName()), true));
            return;
        }

        if (type == null || type.isUnannotatedReference())
            return;

        // This is already handled in readPrimitiveAliasPointer()
        if (target instanceof Alias a && a.isValueWrapper() && type.isPointer())
            return;

        // Pointer to a single value
        builder.addStatement("$1T $2LParam = $2L.reinterpret(($3L).byteSize(), _arena, null)",
                MemorySegment.class, getName(), getValueLayout(type));

        if (type.isPrimitive() || target instanceof Alias a && a.isValueWrapper()) {
            if (type.isLong()) {
                // long and unsigned long
                if (longAsInt)
                    builder.addStatement("$1T _$2LOut = new $3T<>($2LParam.get($4T.JAVA_INT, 0))",
                            getType(), getName(), ClassNames.OUT, ValueLayout.class);
                else
                    builder.addStatement("$1T _$2LOut = new $3T<>((int) $2LParam.get($4T.JAVA_LONG, 0))",
                            getType(), getName(), ClassNames.OUT, ValueLayout.class);
            } else {
                // all other primitive types
                builder.addStatement("$1T _$2LOut = new $3T<>($2LParam.get($4L, 0)$5L)",
                        getType(), getName(), ClassNames.OUT, getValueLayout(type), type.isBoolean() ? " != 0" : "");
            }
        } else {
            var identifier = CodeBlock.builder().add("$LParam", getName());
            if (target instanceof EnumType)
                identifier.add(".get($T.JAVA_INT, 0)", ValueLayout.class);
            builder.addStatement("$T _$LOut = new $T<>($L)",
                    getType(), getName(), ClassNames.OUT, marshalNativeToJava(type, identifier.build()));
        }
    }

    // Ref GObject when ownership is not transferred
    private void refGObject(MethodSpec.Builder builder) {
        if (p.transferOwnership() == TransferOwnership.NONE
                && target != null && target.checkIsGObject()) {
            builder.addStatement("$T.refOnce($L)", ClassNames.INSTANCE_CACHE, getName());
        }
    }

    // Unref user-defined GObject when ownership is transferred to a callback in-parameter
    private void refGObjectUpcall(MethodSpec.Builder builder) {
        if (target != null
                && target.checkIsGObject()
                && p.transferOwnership() == TransferOwnership.FULL
                && !p.isOutParameter()) {
            builder.addStatement("var _$L = $L", getName(), marshalNativeToJava(CodeBlock.of(getName()), true));
            if (target instanceof org.javagi.gir.Class)
                builder.addStatement("$T.unrefUnownedUserDefinedInstance(_$L)", ClassNames.INSTANCE_CACHE, getName());
            else
                builder.beginControlFlow("if (_$L instanceof $T _object)", getName(), ClassNames.G_OBJECT)
                        .addStatement("$T.unrefUnownedUserDefinedInstance(_object)", ClassNames.INSTANCE_CACHE)
                        .endControlFlow();
        }
    }

}
