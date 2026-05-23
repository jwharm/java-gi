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

import org.javagi.javapoet.*;
import org.javagi.configuration.ClassNames;
import org.javagi.gir.*;
import org.javagi.gir.Class;
import org.javagi.gir.Record;

import javax.lang.model.element.Modifier;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.javagi.gir.TransferOwnership.*;
import static org.javagi.util.Conversions.*;

class TypedValueGenerator {

    protected final TypedValue v;
    protected final Array array;
    protected final Type type;
    protected final RegisteredType target;

    TypedValueGenerator(TypedValue v) {
        this.v = v;
        this.array = v.anyType() instanceof Array a ? a : null;
        this.type = v.anyType() instanceof Type t ? t : null;
        this.target = type != null ? type.lookup() : null;
    }

    /**
     * Check if this type can be null
     */
    boolean checkNull() {
        if (v instanceof InstanceParameter)
            return false;

        if (v instanceof Parameter p) {
            if (p.notNull()
                    || p.varargs()
                    || p.isErrorParameter()
                    || p.isUserDataParameter()
                    || p.isDestroyNotifyParameter()
                    || p.isArrayLengthParameter())
                return false;

            if (p.nullable())
                return true;
        }

        if (v instanceof ReturnValue rv) {
            if (rv.anyType().isVoid())
                return false;

            if (rv.nullable())
                return true;
        }

        // A NULL GList is just empty
        if (type != null && type.checkIsGList())
            return false;

        // It is nullable when it is not a primitive type
        return ! (type != null
                    && !type.isPointer()
                    && (type.isPrimitive()
                        || (target instanceof Alias a
                                && a.anyType() instanceof Type t
                                && t.isPrimitive())
                        || target instanceof EnumType));
    }

    /**
     * Check if this type must be annotated as nullable
     */
    boolean annotateNull() {
        if (v instanceof InstanceParameter)
            return false;

        if (v instanceof Parameter p) {
            if (p.notNull()
                    || p.varargs()
                    || p.isErrorParameter()
                    || p.isUserDataParameter()
                    || p.isDestroyNotifyParameter()
                    || p.isArrayLengthParameter())
                return false;

            if (p.nullable())
                return true;
        }

        if (v instanceof ReturnValue rv) {
            if (rv.anyType().isVoid())
                return false;

            if (rv.nullable())
                return true;

            if (rv.notNull())
                return false;

            // Returned arrays are not null
            if (array != null)
                return false;
        }

        // A NULL GList is just empty
        if (type != null && type.checkIsGList())
            return false;

        // All pointer types are nullable by default, except for GError**, but
        // that parameter is hidden from the Java api anyway.
        if (type != null && type.isPointer())
            return false;

        // It is nullable when it is not a primitive type
        return ! (type != null
                && !type.isPointer()
                && (type.isPrimitive()
                || (target instanceof Alias a
                && a.anyType() instanceof Type t
                && t.isPrimitive())
                || target instanceof EnumType));
    }

    TypeName annotated(TypeName typeName) {
        return annotateNull() ? nullable(typeName) : typeName;
    }

    TypeName getType() {
        return getType(true, false);
    }

    TypeName getType(boolean useActualType) {
        return getType(useActualType, false);
    }

    TypeName getAnnotatedType(boolean useActualType) {
        return getType(useActualType, annotateNull());
    }

    private TypeName getType(boolean useActualType, boolean annotate) {
        if (type != null && type.isActuallyAnArray())
            return ArrayTypeName.of(getType(type, useActualType, annotate));

        if (v instanceof Field f && f.callback() != null)
            return f.parent().typeName().nestedClass(
                    toJavaSimpleType(f.name() + "_callback", f.namespace()));

        try {
            return getType(v.anyType(), useActualType, annotate);
        } catch (NullPointerException npe) {
            throw new NoSuchElementException("Cannot find " + type);
        }
    }

    private TypeName getType(AnyType anyType, boolean useActualType, boolean annotate) {
        // Wrap out parameters in an Out<>, except for primitive aliases, they
        // are already "wrapped" in an Alias<>
        if (v instanceof Parameter p && p.isOutParameter()
                && (! (p.direction() != Direction.IN && v.isValueWrapper()))) {
            TypeName typeName = anyType.typeName();

            if (useActualType && v.isBitfield())
                typeName = ParameterizedTypeName.get(ClassName.get(Set.class), typeName.box());

            return annotated(ParameterizedTypeName.get(ClassNames.OUT, typeName.box()));
        }

        TypeName typeName = (annotate && annotateNull())
                ? anyType.nullableAnnotatedTypeName()
                : anyType.typeName();

        if (useActualType && v.isBitfield())
                typeName = ParameterizedTypeName.get(ClassName.get(Set.class), anyType.typeName().box());

        if (!useActualType && type != null && type.isFilename() && !(v instanceof Parameter p && p.isOutParameter()))
            typeName = TypeName.get(String.class);

        if (type != null && type.isUnannotatedReference())
            return annotated(TypeName.get(MemorySegment.class));

        return typeName;
    }

    String getName() {
        return "...".equals(v.name()) ? "varargs" : toJavaIdentifier(v.name());
    }

    CodeBlock transfer() {
        TransferOwnership transfer = switch(v) {
            case Parameter    p ->  p.transferOwnership();
            case ReturnValue rv -> rv.transferOwnership();
            default -> NONE;
        };
        return CodeBlock.of("$T.$L", ClassNames.TRANSFER_OWNERSHIP, transfer.toString());
    }

    CodeBlock marshalJavaToNative(CodeBlock identifier) {
        if (type != null)
            return marshalJavaToNative(type, identifier);

        if (array != null)
            return marshalJavaArrayToNative(array, identifier);

        if (v instanceof Field f && f.callback() != null)
            return identifier.toBuilder().add(".toCallback(_arena)").build();

        return CodeBlock.of("$T.NULL /* unsupported */", MemorySegment.class);
    }

    private CodeBlock marshalJavaToNative(Type type, CodeBlock identifier) {
        if (type.isActuallyAnArray())
            return CodeBlock.of("$T.allocate($L, false, _arena)", ClassNames.INTEROP, identifier);

        if (type.isString()) {
            if (v.transferOwnership() == FULL)
                return CodeBlock.of("$T.allocateUnowned($L)", ClassNames.INTEROP, identifier);
            else
                return CodeBlock.of("$T.allocate($L, _arena)", ClassNames.INTEROP, identifier);
        }

        if (type.isFilename())
            return CodeBlock.of("$L.toMemorySegment(_arena)", identifier);

        if (! (v instanceof Parameter p && p.isOutParameter()))
            if (type.cType() != null && type.cType().endsWith("**"))
                return identifier;

        if (type.isMemorySegment())
            return identifier;

        if (type.isBoolean())
            return CodeBlock.of("$L ? 1 : 0", identifier);

        if (type.isUnannotatedReference())
            return CodeBlock.of("$L /* missing annotation */", identifier);

        return marshalJavaToNative(target, identifier);
    }

    private CodeBlock marshalJavaToNative(RegisteredType target, CodeBlock identifier) {
        return switch (target) {
            case null -> identifier;
            case Alias alias when alias.anyType() instanceof Array ->
                    CodeBlock.of("$T.allocate($L, true, _arena)", ClassNames.INTEROP, identifier);
            case Alias alias when alias.anyType() instanceof Type t -> {
                RegisteredType typedef = t.lookup();
                if (typedef != null)
                    yield marshalJavaToNative(typedef, identifier);

                if (t.isString() || t.isMemorySegment() || t.isVoid())
                    yield CodeBlock.of("$L.getValue()", identifier);

                yield CodeBlock.of("$L.getValue().$LValue()", identifier, t.javaType());
            }
            case Bitfield _ -> CodeBlock.of("$T.enumSetToInt($L)", ClassNames.INTEROP, identifier);
            case Callback _ -> {
                CodeBlock.Builder arena = CodeBlock.builder();
                switch(Scope.ofTypedValue(v)) {
                    case null -> arena.add("$T.global()", Arena.class);
                    case BOUND -> arena.add("$T.attachArena($T.ofConfined(), this)", ClassNames.INTEROP, Arena.class);
                    case CALL -> arena.add("_arena");
                    case NOTIFIED -> arena.add(getNotifiedCallbackScope(identifier));
                    case ASYNC -> arena.add("_$LScope", identifier);
                    case FOREVER -> arena.add("$T.global()", Arena.class);
                }
                yield CodeBlock.of("$L.toCallback($L)", identifier, arena.build());
            }
            case Enumeration _ -> CodeBlock.of("$L.getValue()", identifier);
            case Record rec when rec.checkIsGBytes() -> {
                if (v instanceof ReturnValue)
                    yield CodeBlock.of("$T.toGBytes($L)", ClassNames.INTEROP, identifier);
                else
                    yield CodeBlock.of("_$LGBytes", getName());
            }
            case Record rec when rec.checkIsGString() -> {
                if (v instanceof ReturnValue)
                    yield CodeBlock.of("$T.toGString($L)", ClassNames.INTEROP, identifier);
                else
                    yield CodeBlock.of("_$LGString", getName());
            }
            default -> CodeBlock.of("$L.handle()", identifier);
        };
    }

    /*
     * Sometimes a method has multiple callback arguments (such as a "progress
     * callback" and an AsyncReadyCallback) with a single shared DestroyNotify
     * argument. In that case, we can use the arena of the other callback.
     */
    private CodeBlock getNotifiedCallbackScope(CodeBlock identifier) {
        if (v instanceof Parameter p && p.destroy() == null)
            return p.parent().parameters().stream()
                    .filter(other -> p != other
                            && ((other.scope() == Scope.NOTIFIED && other.destroy() != null)
                              || other.scope() == Scope.ASYNC)
                            && other.anyType() instanceof Type t
                            && t.lookup() instanceof Callback)
                    .findAny()
                    .map(other -> CodeBlock.of("_$LScope", new TypedValueGenerator(other).getName()))
                    .orElseGet(() -> CodeBlock.of("$T.global()", Arena.class)); // Fallback to global arena
        else
            return CodeBlock.of("_$LScope", identifier);
    }

    private CodeBlock marshalJavaArrayToNative(Array array, CodeBlock identifier) {
        if (array == null || array.anyType() == null)
            return CodeBlock.of("$T.NULL /* unsupported */", MemorySegment.class);

        CodeBlock mallocAllocator = CodeBlock.of("$T.mallocAllocator()", ClassNames.INTEROP);
        CodeBlock arenaAllocator = CodeBlock.of("_arena");

        // When ownership is transferred, we must not free the allocated
        // memory -> use global scope
        TransferOwnership transfer = v instanceof Parameter p ? p.transferOwnership() : NONE;
        CodeBlock allocator = (transfer == CONTAINER || transfer == FULL) ? mallocAllocator : arenaAllocator;

        // String[][]
        if (array.anyType() instanceof Array inner
                && inner.anyType() instanceof Type t && t.isString()) {
            return CodeBlock.of("$T.allocate($L, $L, $L, $L)",
                            ClassNames.INTEROP,
                            identifier,
                            array.zeroTerminated(),
                            allocator,
                            transfer == FULL ? mallocAllocator : arenaAllocator);
        }

        Type elemType = (Type) array.anyType();
        RegisteredType target = elemType.lookup();
        boolean isEnum = target instanceof EnumType;
        boolean isPrimitiveAlias = target instanceof Alias a && a.isValueWrapper();
        String primitiveClassName = isPrimitiveAlias
                ? primitiveClassName(((Alias) target).anyType().typeName().toString())
                : "";

        CodeBlock stmt;

        if (isEnum || isPrimitiveAlias)
            stmt = CodeBlock.of("$T.allocate($T.get$LValues($L), $L, $L)",
                    ClassNames.INTEROP,
                    isEnum ? ClassNames.INTEROP : elemType.typeName(),
                    primitiveClassName,
                    identifier,
                    array.zeroTerminated(),
                    allocator);

        else if (target instanceof Record
                        && (!elemType.isPointer())
                        && (!"GLib.PtrArray".equals(array.name())))
            stmt = CodeBlock.of("$T.allocate($L, $T.getMemoryLayout(), $L, $L)",
                            ClassNames.INTEROP,
                            identifier,
                            target.typeName(),
                            array.zeroTerminated(),
                            allocator);

        else if ("GLib.ByteArray".equals(array.name())) {
            stmt = CodeBlock.of("$T.$L($L).handle()",
                    ClassNames.G_BYTE_ARRAY,
                    transfer != NONE ? "takeUnowned" : "take",
                    identifier);
        }

        else
            stmt = CodeBlock.of("$T.allocate($L, $L, $L)",
                            ClassNames.INTEROP,
                            identifier,
                            array.zeroTerminated(),
                            allocator);

        // GArray
        // TODO: when ownership is not transferred, unref the GArray/GPtrArray
        if (array.name() != null && "GLib.Array".equals(array.name())) {
            CodeBlock elemSize;
            if (elemType.isLong())
                elemSize = CodeBlock.of("$T.longAsInt() ? 4 : 8", ClassNames.INTEROP);
            else
                elemSize = CodeBlock.of("" + array.anyType().allocatedSize(false));
            return CodeBlock.of("$T.newGArray($L, $L.length, $L)", ClassNames.INTEROP, stmt, identifier, elemSize);
        }

        // GPtrArray
        // TODO: when ownership is transferred, set the element_free_func
        if (array.name() != null && "GLib.PtrArray".equals(array.name())) {
            return CodeBlock.of("$T.newGPtrArray($L, $L.length)", ClassNames.INTEROP, stmt, identifier);
        }

        return stmt;
    }

    CodeBlock marshalNativeToJava(CodeBlock identifier, boolean upcall) {
        if (type != null) {
            if ("gfloat**".equals(type.cType()))
                return CodeBlock.of("null /* unsupported */");

            if (type.isActuallyAnArray()) {
                String size = v instanceof Field ? "length" : null;
                return marshalNativeToJavaArray(type, size, identifier);
            }

            if (type.isUnannotatedReference())
                return CodeBlock.of("$L /* missing annotation */", identifier);

            return marshalNativeToJava(type, identifier);
        }

        // String[][]
        if (array != null && array.anyType() instanceof Array inner
                && inner.anyType() instanceof Type t && t.isString()) {
            String size = array.sizeExpression(upcall);
            if (size == null)
                return CodeBlock.of("$T.getStrvArray($L, $L)",
                        ClassNames.INTEROP, identifier, transfer());
            else
                return CodeBlock.of("$T.getStrvArray($L, $L, $L)",
                        ClassNames.INTEROP, identifier, size, transfer());
        }

        // Array
        if (array != null && array.anyType() instanceof Type t)
             return marshalNativeToJavaArray(t, array.sizeExpression(upcall), identifier);

        // Nested array
        return CodeBlock.of("null /* unsupported */");
    }

    CodeBlock marshalNativeToJava(Type type, CodeBlock identifier) {
        boolean isTypeInstance = target instanceof Record && "TypeInstance".equals(target.name());
        boolean isTypeClass = target instanceof Record && "TypeClass".equals(target.name());

        // Get parent node (parameter, return value, ...)
        Node parent = type.parent();
        while (parent instanceof AnyType)
            parent = parent.parent();

        // Find out how ownership is transferred
        var transferOwnership = switch(parent) {
            case Parameter p         -> p.transferOwnership();
            case InstanceParameter i -> i.transferOwnership();
            case ReturnValue r       -> r.transferOwnership();
            case Property p          -> p.transferOwnership();
            case Field _, Constant _ -> NONE;
            default                  -> throw new IllegalStateException();
        };

        if (type.isString())
            return CodeBlock.of("$T.getString($L, $L)", ClassNames.INTEROP, identifier, transfer());

        if (type.isFilename())
            return CodeBlock.of("new $T($L, $L)", ClassNames.FILENAME, identifier, transfer());

        if (target instanceof EnumType)
            return CodeBlock.of("$T.of($L)", target.typeName(), identifier);

        // Generate constructor call for GList/GSList with generic element types
        if (target != null && type.checkIsGList()) {
            if (type.anyTypes().size() != 1)
                throw new UnsupportedOperationException("Unsupported element type: " + type);

            // Generate lambdas or method references to create and destruct elements
            CodeBlock elementConstructor = getElementConstructor(type, 0);
            CodeBlock elementDestructor = getElementDestructor(type, 0);

            var stmt = CodeBlock.builder().add("new $T($L, $L", type.typeName(), identifier, elementConstructor);

            if (elementDestructor != null)
                stmt.add(", $L", elementDestructor);

            return stmt.add(", $T.$L)", ClassNames.TRANSFER_OWNERSHIP, transferOwnership)
                    .build();
        }

        // Generate constructor call for HashTable with generic types for keys and values
        if (target != null && type.checkIsGHashTable()) {
            if (type.anyTypes().size() != 2)
                throw new UnsupportedOperationException("Unsupported element type: " + type);

            CodeBlock keyConstructor = getElementConstructor(type, 0);
            CodeBlock valueConstructor = getElementConstructor(type, 1);

            return CodeBlock.of("new $T($L, $L, $L)", type.typeName(), identifier, keyConstructor, valueConstructor);
        }

        if (target != null && target.checkIsGBytes())
            return CodeBlock.of("$T.fromGBytes($L)", ClassNames.INTEROP, identifier);

        if (target != null && target.checkIsGString())
            return CodeBlock.of("$T.fromGString($L, $T.$L)",
                            ClassNames.INTEROP, identifier, ClassNames.TRANSFER_OWNERSHIP, transferOwnership);

        if ((target instanceof Record && !isTypeInstance && !isTypeClass)
                || target instanceof Union
                || target instanceof Boxed
                || (target instanceof Alias a && a.lookup() instanceof Record))
            return CodeBlock.of("$1T.NULL.equals($2L) ? null : new $3T($2L)",
                    MemorySegment.class, identifier, target.typeName());

        if (target instanceof Alias a && a.isValueWrapper())
            return CodeBlock.of("new $T($L)", target.typeName(), identifier);

        if (target instanceof Callback)
            return CodeBlock.of("null /* Unsupported parameter type */");

        String cacheFunction = isTypeClass ? "getTypeClass" : "get";

        if (target instanceof Class
                || target instanceof Interface
                || (target instanceof Alias a && a.isProxy())
                || isTypeInstance
                || isTypeClass)
            return CodeBlock.of("($T) $T.$L($L, $L)",
                    target.typeName(), ClassNames.INSTANCE_CACHE, cacheFunction, identifier, target.constructorName());

        if (type.isBoolean())
            return CodeBlock.of("$L != 0", identifier);

        return identifier;
    }

    private static CodeBlock getElementConstructor(Type type, int child) {
        return switch (type.anyTypes().get(child)) {
            case Type t when t.isString() ->
                CodeBlock.of("$T::getString", ClassNames.INTEROP);
            case Type t when t.isFilename() ->
                CodeBlock.of("pointer -> new $T(pointer, $T.NONE)",
                        ClassNames.FILENAME, ClassNames.TRANSFER_OWNERSHIP);
            // GPOINTER_TO_INT()
            case Type t when t.isInt32() && !t.isPointer() ->
                CodeBlock.of("pointer -> (int) pointer.address()");
            case Type t when t.isPrimitive() ->
                CodeBlock.of("$T::get$L", ClassNames.INTEROP, primitiveClassName(t.javaType()));
            case Type t when t.isMemorySegment() ->
                CodeBlock.of("(_p -> _p)");
            case Array _ ->
                CodeBlock.of("(_p -> _p)");
            case Type t when t.lookup() != null -> {
                var elemTarget = t.lookup();
                // For enum types (bitfield/enumeration) read an integer and call <EnumType>.of()
                if (elemTarget instanceof EnumType enumType) {
                    yield CodeBlock.of("(_ptr -> $T.of($T.getInteger(_ptr)))",
                            enumType.typeName(), ClassNames.INTEROP);
                } else if (elemTarget.checkIsGObject()) {
                    yield CodeBlock.of("_ptr -> ($T) $T.get(_ptr, $L)",
                            elemTarget.typeName(), ClassNames.INSTANCE_CACHE, elemTarget.constructorName());
                } else {
                    yield elemTarget.constructorName();
                }
            }
            default ->
                throw new UnsupportedOperationException("Unsupported element type: " + type);
        };
    }

    private static CodeBlock getElementDestructor(Type type, int child) {
        return switch (type.anyTypes().get(child)) {
            case Array _ ->
                CodeBlock.of("(_ -> {}) /* unsupported */");
            case Type t when t.isString() ->
                null;
            case Type t when t.isFilename() ->
                null;
            case Type t when t.isPrimitive() ->
                null;
            case Type t when t.isMemorySegment() ->
                CodeBlock.of("$T::free", ClassNames.G_LIB);
            case Type t when t.lookup() != null ->
                t.lookup().destructorName();
            default ->
                throw new UnsupportedOperationException("Unsupported element type: " + type);
        };
    }

    private CodeBlock marshalNativeToJavaArray(Type type, String arraySize, CodeBlock identifier) {
        RegisteredType target = type.lookup();
        String primitive = type.isPrimitive() ? primitiveClassName(type.javaType()) : null;
        CodeBlock size = arraySize == null ? null : CodeBlock.of(arraySize);

        // GArray stores the length in the len field
        if (size == null
                && array != null
                && array.name() != null
                && List.of("GLib.Array", "GLib.PtrArray", "GLib.ByteArray").contains(array.name())) {
            size = CodeBlock.of("new $T($L).readLen()",
                    toJavaQualifiedType(array.name(), array.namespace()), identifier);
            identifier = CodeBlock.of("$T.dereference($L)", ClassNames.INTEROP, identifier);
        }

        // Null-terminated array
        if (size == null) {
            if (type.isString())
                return CodeBlock.of("$T.getStringArray($L, $L)",
                        ClassNames.INTEROP, identifier, transfer());

            if (type.isFilename())
                return CodeBlock.of("$T.getFilenameArray($L, $L)",
                        ClassNames.INTEROP, identifier, transfer());

            if (type.isMemorySegment())
                return CodeBlock.of("$T.getAddressArray($L, $L)",
                        ClassNames.INTEROP, identifier, transfer());

            if (target instanceof EnumType)
                return CodeBlock.of("$T.getArrayFromIntPointer($L, (int) $1T.class, $1T::of)",
                        ClassNames.INTEROP, identifier, target.typeName());

            if (target instanceof Alias a && a.isValueWrapper())
                return CodeBlock.of("$T.fromNativeArray($L, length, $L)",
                        target.typeName(), identifier, transfer());

            if (type.isPrimitive())
                return CodeBlock.of("$T.get$LArray($L, $L)",
                        ClassNames.INTEROP, primitive, identifier, transfer());

            if (target instanceof Record && (! type.isPointer()) &&
                    (! (array != null && "GLib.PtrArray".equals(array.name()))))
                return CodeBlock.of("$1T.getStructArray($2L, $3T.class, $4L, $3T.getMemoryLayout())",
                        ClassNames.INTEROP, identifier, target.typeName(), target.constructorName());

            if (target == null)
                throw new IllegalStateException("Target is null for type " + type);

            return CodeBlock.of("$T.getProxyArray($L, $T.class, $L)",
                            ClassNames.INTEROP, identifier, target.typeName(), target.constructorName());
        }

        // Array with known size
        if (type.isString())
            return CodeBlock.of("$T.getStringArray($L, $L, $L)",
                    ClassNames.INTEROP, identifier, size, transfer());

        if (type.isFilename())
            return CodeBlock.of("$T.getFilenameArray($L, $L, $L)",
                    ClassNames.INTEROP, identifier, size, transfer());

        if (type.isMemorySegment())
            return CodeBlock.of("$T.getAddressArray($L, $L, $L)",
                    ClassNames.INTEROP, identifier, size, transfer());

        if (target instanceof EnumType)
            return CodeBlock.of("$1T.getArrayFromIntPointer($2L, (int) $3L, $4T.class, $4T::of)",
                    ClassNames.INTEROP, identifier, size, target.typeName());

        if (target instanceof Alias a && a.isValueWrapper())
            return CodeBlock.of("$T.fromNativeArray($L, $L, $L)",
                    target.typeName(), identifier, size, transfer());

        if (type.isPrimitive() && array != null && array.anyType() instanceof Type)
            return CodeBlock.of("$T.get$LArray($L, $L, $L)",
                    ClassNames.INTEROP, primitive, identifier, size, transfer());

        if (type.isPrimitive())
            return CodeBlock.of("$T.get$LArray($L, $L, $L)",
                    ClassNames.INTEROP, primitive, identifier, size, transfer());

        if (target instanceof Record && (! type.isPointer()) &&
                (! (array != null && "GLib.PtrArray".equals(array.name()))))
            return CodeBlock.of("$1T.getStructArray($2L, (int) $3L, $4T.class, $5L, $4T.getMemoryLayout())",
                    ClassNames.INTEROP, identifier, size, target.typeName(), target.constructorName());

        if (target == null)
            throw new IllegalStateException("Target is null for type " + type);

        return CodeBlock.of("$T.getProxyArray($L, (int) $L, $T.class, $L)",
                ClassNames.INTEROP, identifier, size, target.typeName(), target.constructorName());
    }

    CodeBlock getGTypeDeclaration() {
        if (array != null) {
            if (array.anyType() instanceof Type arrayType && "utf8".equals(arrayType.name()))
                return CodeBlock.of("$T.STRV", ClassNames.TYPES);

            if ("GLib.ByteArray".equals(array.name()))
                return CodeBlock.of("$T.getType()", ClassNames.G_BYTE_ARRAY);

            // Other array types are not supported yet, but could be added here
            return CodeBlock.of("$T.POINTER /* unsupported */", ClassNames.TYPES);
        }

        if (type == null)
            throw new IllegalStateException("Expected type or array");

        if (type.isPrimitive())
            return CodeBlock.of("$T.$L", ClassNames.TYPES, switch(type.cType()) {
                case "gboolean" -> "BOOLEAN";
                case "gchar", "gint8" -> "CHAR";
                case "guchar", "guint8" -> "UCHAR";
                case "gint", "gint32" -> "INT";
                case "guint", "guint32", "gunichar" -> "UINT";
                case "glong" -> "LONG";
                case "gulong" -> "ULONG";
                case "gint64" -> "INT64";
                case "guint64" -> "UINT64";
                case "gpointer", "gconstpointer", "gssize", "gsize", "goffset", "gintptr", "guintptr" -> "POINTER";
                case "gdouble" -> "DOUBLE";
                case "gfloat" -> "FLOAT";
                case "none" -> "NONE";
                case "utf8", "filename" -> "STRING";
                default -> throw new IllegalStateException("Unexpected type: " + type.cType());
            });

        if (type.isString())
            return CodeBlock.of("$T.STRING", ClassNames.TYPES);

        if (type.isMemorySegment())
            return CodeBlock.of("$T.POINTER", ClassNames.TYPES);

        if (type.typeName().equals(ClassNames.G_OBJECT))
            return CodeBlock.of("$T.OBJECT", ClassNames.TYPES);

        RegisteredType rt = target instanceof Alias a ? a.lookup() : target;

        if (rt != null) {
            if (rt instanceof Class cls && cls.isInstanceOf("GObject", "ParamSpec"))
                return CodeBlock.of("$T.PARAM", ClassNames.TYPES);

            if (rt.javaType().equals("org.gnome.glib.Variant"))
                return CodeBlock.of("$T.VARIANT", ClassNames.TYPES);

            if (rt.checkIsGBytes())
                return CodeBlock.of("$T.BYTES", ClassNames.TYPES);

            if (rt.checkIsGString())
                return CodeBlock.of("$T.GSTRING", ClassNames.TYPES);

            if (rt.checkIsGHashTable())
                return CodeBlock.of("$T.HASH_TABLE", ClassNames.TYPES);

            if (rt.getTypeFunc() != null)
                return CodeBlock.of("$T.getType()", type.typeName());
        }

        if (type.typeName().equals(ClassNames.G_TYPE))
            return CodeBlock.of("$T.gtypeGetType()", ClassNames.G_OBJECTS);

        return CodeBlock.of("$T.POINTER", ClassNames.TYPES);
    }

    CodeBlock getValueSetter(CodeBlock payloadIdentifier) {
        if (array != null) {
            // GStrv is just an alias for an array of strings, but GByteArray
            // needs to be allocated
            if ("GLib.ByteArray".equals(array.name()))
                return CodeBlock.of("_value.setBoxed($T.takeUnowned($L).handle())",
                        ClassNames.G_BYTE_ARRAY, payloadIdentifier);
            else
                return CodeBlock.of("_value.setBoxed($T.allocate($L, true, _arena))",
                        ClassNames.INTEROP, payloadIdentifier);
        }

        // GBytes and GString have their own marshalling functions in the Interop class
        if (target != null && target.checkIsGBytes())
            return CodeBlock.of("_value.setBoxed($T.toGBytes($L))",
                    ClassNames.INTEROP, payloadIdentifier);

        if (target != null && target.checkIsGString())
            return CodeBlock.of("_value.setBoxed($T.toGString($L))",
                    ClassNames.INTEROP, payloadIdentifier);

        // Check for fundamental classes with their own GValue setters
        if (type != null) {
            RegisteredType rt = target instanceof Alias a ? a.lookup() : target;

            if (rt instanceof Class cls && cls.setValueFunc() != null) {
                var setter = cls.namespace().functions().stream()
                        .filter(f -> f.callableAttrs().cIdentifier().equals(cls.setValueFunc()))
                        .findAny();

                if (setter.isPresent()) {
                    Function function = setter.get();
                    String setValueFunc = toJavaIdentifier(function.name());
                    ClassName globalClass = toJavaQualifiedType(rt.namespace().globalClassName(), rt.namespace());
                    return CodeBlock.of("$T.$L(_value, $L)", globalClass, setValueFunc, payloadIdentifier);
                }
            }
        }

        // Other, known types
        String setValue = null;
        if (type != null) {
            setValue = switch (type.cType()) {
                case "gboolean" -> "setBoolean";
                case "gchar", "gint8" -> "setSchar";
                case "guchar", "guint8" -> "setUchar";
                case "gint", "gint32" -> "setInt";
                case "guint", "guint32", "gunichar" -> "setUint";
                case "glong" -> "setLong";
                case "gulong" -> "setUlong";
                case "gint64" -> "setInt64";
                case "guint64" -> "setUint64";
                case "gpointer", "gconstpointer", "gssize", "gsize", "goffset", "gintptr", "guintptr" -> "setPointer";
                case "gdouble" -> "setDouble";
                case "gfloat" -> "setFloat";
                case "none" -> "NONE";
                case "utf8", "filename" -> "setString";
                case null, default -> null;
            };

            if (type.isString())
                setValue = "setString";
            else if (type.isMemorySegment() || (type.name() == null && type.cType() == null))
                setValue = "setPointer";
            else if (type.typeName().equals(ClassNames.G_OBJECT))
                setValue = "setObject";
            else if (type.checkIsGList())
                setValue = "setPointer";
        }

        if (setValue == null) {
            RegisteredType rt = target instanceof Alias a ? a.lookup() : target;

            if (rt != null) {
                if (rt instanceof Class cls && cls.isInstanceOf("GObject", "ParamSpec"))
                    setValue = "setParam";

                else if (rt.javaType().equals("org.gnome.glib.Variant"))
                    setValue = "setVariant";

                else if (rt.checkIsGBytes() || rt.checkIsGString() || rt.checkIsGHashTable())
                    setValue = "setBoxed";

                else if (rt instanceof Record r && r.getTypeFunc() != null)
                    setValue = "setBoxed";
            }

            if (setValue == null) {
                if (type != null && type.typeName().equals(ClassNames.G_TYPE))
                    setValue = "setGtype";
                else if (target instanceof Enumeration)
                    setValue = "setEnum";
                else if (target instanceof Bitfield)
                    setValue = "setFlags";
                else if (target instanceof Record)
                    setValue = "setBoxed";
                else
                    setValue = "setObject";
            }
        }

        return switch(setValue) {
            case "setEnum" ->
                    CodeBlock.of("_value.$L($L.getValue())",
                            setValue, payloadIdentifier);
            case "setFlags" ->
                    CodeBlock.of("_value.$L($T.enumSetToInt($L))",
                            setValue, ClassNames.INTEROP, payloadIdentifier);
            case "setBoxed", "setPointer" ->
                    CodeBlock.of("_value.$L($L)",
                            setValue, marshalJavaToNative(payloadIdentifier));
            case "setObject" ->
                    CodeBlock.of("_value.$L(($T) $L)",
                            setValue, ClassNames.G_OBJECT, payloadIdentifier);
            default ->
                    CodeBlock.of("_value.$L($L)",
                            setValue, payloadIdentifier);
        };
    }

    FieldSpec generateConstantDeclaration() {
        final Constant c = (Constant) v;
        final String value = c.value();
        try {
            // Static field
            var builder = FieldSpec.builder(getType(true), toJavaConstant(c.name()),
                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

            // Javadoc
            if (c.infoElements().doc() != null)
                builder.addJavadoc(new DocGenerator(c.infoElements().doc()).generate());

            // Deprecated annotation
            if (c.callableAttrs().deprecated())
                builder.addAnnotation(Deprecated.class);

            // Constant value initializer
            if (target instanceof Alias a && a.isValueWrapper())
                builder.initializer("new $T($L)", a.typeName(), literal(a.anyType().typeName(), value));
            else if (target instanceof EnumType)
                builder.initializer(marshalNativeToJava(CodeBlock.of(literal(TypeName.INT, value)), false));
            else
                builder.initializer(literal(type.typeName(), value).replace("$", "$$"));

            // Build the static field
            return builder.build();

        } catch (NumberFormatException nfe) {
            // Do not write anything
            System.out.printf("Skipping <constant name=\"%s\" value=\"%s\">: %s%n",
                    c.name(), value, nfe.getMessage());
            return null;
        }
    }

    // Generate a statement that represents ValueLayout for this type.
    // Returns ValueLayout.ADDRESS for complex layouts.
    CodeBlock getValueLayout(AnyType anyType) {
        return anyType instanceof Type t && t.isLong()
                ? CodeBlock.of("$1T.longAsInt() ? $2T.JAVA_INT : $2T.JAVA_LONG",
                        ClassNames.INTEROP, ValueLayout.class)
                : getValueLayoutPlain(anyType, false);
    }

    // Generate a statement that retrieves a ValueLayout for primitive types,
    // or the MemoryLayout of complex types.
    CodeBlock getMemoryLayout(AnyType anyType) {
        if (anyType instanceof Type t && (!t.isPrimitive())) {
            var target = t.lookup();
            if (target instanceof StandardLayoutType && target instanceof FieldContainer fc) {
                if (new MemoryLayoutGenerator().canGenerate(fc)) {
                    return CodeBlock.of("$T.getMemoryLayout()", target.typeName());
                }
            }
        }
        return getValueLayout(anyType);
    }
}
