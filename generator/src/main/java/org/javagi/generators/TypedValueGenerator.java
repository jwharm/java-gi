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
import org.javagi.gir.*;
import org.javagi.util.Conversions;
import org.javagi.util.PartialStatement;
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
     * We cannot null-check primitive values.
     * @return true if this parameter is not a primitive value
     */
    boolean checkNull() {
        if (v instanceof InstanceParameter)
            return false;

        if (v instanceof Parameter p &&
                (p.notNull()
                    || p.varargs()
                    || p.isErrorParameter()
                    || p.isUserDataParameter()
                    || p.isDestroyNotifyParameter()
                    || p.isArrayLengthParameter()))
            return false;

        // A NULL GList is just empty
        if (type != null && type.checkIsGList())
            return false;

        return ! (type != null
                    && !type.isPointer()
                    && (type.isPrimitive()
                        || (target instanceof Alias a
                                && a.anyType() instanceof Type t
                                && t.isPrimitive())
                        || target instanceof EnumType));
    }

    String transfer() {
        TransferOwnership transfer = switch(v) {
            case Parameter    p ->  p.transferOwnership();
            case ReturnValue rv -> rv.transferOwnership();
            default -> NONE;
        };
        return "$transferOwnership:T." + transfer.toString();
    }

    TypeName getType() {
        return getType(true);
    }

    TypeName getType(boolean setOfBitfield) {
        if (type != null && type.isActuallyAnArray())
            return ArrayTypeName.of(getType(type, setOfBitfield));

        if (v instanceof Field f && f.callback() != null)
            return f.parent().typeName().nestedClass(
                    toJavaSimpleType(f.name() + "_callback", f.namespace()));

        try {
            return getType(v.anyType(), setOfBitfield);
        } catch (NullPointerException npe) {
            throw new NoSuchElementException("Cannot find " + type);
        }
    }

    private TypeName getType(AnyType anyType, boolean setOfBitfield) {
        // Wrap Bitfield return value into a Set<>
        TypeName typeName = anyType.typeName();
        typeName = (setOfBitfield && v.isBitfield())
                ? ParameterizedTypeName.get(ClassName.get(Set.class), typeName)
                : typeName;

        if (v instanceof Parameter p && p.isOutParameter())
            return ParameterizedTypeName.get(ClassNames.OUT, typeName.box());

        if (type != null
                && type.isPointer()
                && (type.isPrimitive() || target instanceof EnumType))
            return TypeName.get(MemorySegment.class);

        return typeName;
    }

    String getName() {
        return "...".equals(v.name()) ? "varargs" : toJavaIdentifier(v.name());
    }

    PartialStatement marshalJavaToNative(String identifier) {
        if (type != null)
            return marshalJavaToNative(type, identifier);

        if (array != null && array.anyType() instanceof Array)
            return PartialStatement.of(
                    "$memorySegment:T.NULL /* unsupported */",
                    "memorySegment", MemorySegment.class);

        if (array != null && array.anyType() instanceof Type)
            return marshalJavaArrayToNative(array, identifier);

        if (v instanceof Field f && f.callback() != null)
            return PartialStatement.of(identifier + ".toCallback(_arena)");

        return PartialStatement.of(
                "$memorySegment:T.NULL /* unsupported */",
                "memorySegment", MemorySegment.class);
    }

    private PartialStatement marshalJavaToNative(Type type, String identifier) {
        if (type.isActuallyAnArray())
            return PartialStatement.of(
                    "$interop:T.allocateNativeArray(" + identifier + ", false, _arena)",
                    "interop", ClassNames.INTEROP);

        if (type.isString()) {
            if (v.transferOwnership() == FULL)
                return PartialStatement.of(
                        "$interop:T.allocateUnownedString(" + identifier + ")",
                        "interop", ClassNames.INTEROP);
            else
                return PartialStatement.of(
                        "$interop:T.allocateNativeString(" + identifier + ", _arena)",
                        "interop", ClassNames.INTEROP);
        }

        if (! (v instanceof Parameter p && p.isOutParameter()))
            if (type.cType() != null && type.cType().endsWith("**"))
                return PartialStatement.of(identifier);

        if (type.isMemorySegment())
            return PartialStatement.of(identifier);

        if (type.isBoolean())
            return PartialStatement.of(identifier + " ? 1 : 0");

        return marshalJavaToNative(target, identifier);
    }

    private PartialStatement marshalJavaToNative(RegisteredType target, String identifier) {
        return switch (target) {
            case null -> PartialStatement.of(identifier);
            case Alias alias when alias.anyType() instanceof Array ->
                    PartialStatement.of(
                            "$interop.allocateNativeArray(" + identifier + ", true, _arena)",
                            "interop", ClassNames.INTEROP);
            case Alias alias when alias.anyType() instanceof Type t -> {
                RegisteredType typedef = t.lookup();
                if (typedef != null)
                    yield marshalJavaToNative(typedef, identifier);
                String stmt = switch(toJavaBaseType(t.name())) {
                    case "String", "MemorySegment", "void" -> identifier + ".getValue()";
                    default -> identifier + ".getValue()." + t.typeName() + "Value()";
                };
                yield PartialStatement.of(stmt);
            }
            case Bitfield _ -> PartialStatement.of(
                    "$interop:T.enumSetToInt(" + identifier + ")",
                    "interop", ClassNames.INTEROP);
            case Callback _ -> {
                String arena = switch(Scope.ofTypedValue(v)) {
                    case null -> "$arena:T.global()";
                    case BOUND -> "$interop:T.attachArena($arena:T.ofConfined(), this)";
                    case CALL -> "_arena";
                    case NOTIFIED, ASYNC -> "_" + identifier + "Scope";
                    case FOREVER -> "$arena:T.global()";
                };
                yield PartialStatement.of(
                        identifier + ".toCallback(" + arena + ")",
                        "arena", Arena.class,
                        "interop", ClassNames.INTEROP);
            }
            case Enumeration _ -> PartialStatement.of(identifier + ".getValue()");
            case Record rec when rec.checkIsGBytes() -> {
                if (v instanceof ReturnValue) {
                    yield PartialStatement.of("$interop:T.toGBytes(" + identifier + ")",
                            "interop", ClassNames.INTEROP);
                } else {
                    yield PartialStatement.of("_" + getName() + "GBytes");
                }
            }
            default -> PartialStatement.of(identifier + ".handle()");
        };
    }

    private PartialStatement marshalJavaArrayToNative(Array array, String identifier) {
        // When ownership is transferred, we must not free the allocated
        // memory -> use global scope
        String allocator = (v instanceof Parameter p && p.transferOwnership() != NONE)
                ? "$arena:T.global()" : "_arena";

        Type type = (Type) array.anyType();
        RegisteredType target = type.lookup();

        boolean isEnum = target instanceof EnumType;
        boolean isPrimitiveAlias = target instanceof Alias a && a.isValueWrapper();

        String targetTypeTag = isEnum ? "enumType" : type.toTypeTag();

        String primitiveClassName = isPrimitiveAlias
                ? primitiveClassName(((Alias) target).anyType().typeName().toString())
                : "";

        PartialStatement stmt;

        if (isEnum || isPrimitiveAlias)
            stmt = PartialStatement.of(
                    "$interop:T.allocateNativeArray($" + targetTypeTag + ":T.get"
                            + primitiveClassName + "Values(" + identifier + "), "
                            + array.zeroTerminated() + ", " + allocator + ")",
                    "arena", Arena.class,
                    "interop", ClassNames.INTEROP,
                    targetTypeTag, isEnum ? ClassNames.INTEROP : type.typeName());

        else if (target instanceof Record && (!type.isPointer()))
            stmt = PartialStatement.of(
                    "$interop:T.allocateNativeArray(" + identifier
                            + ", $" + targetTypeTag + ":T.getMemoryLayout(), "
                            + array.zeroTerminated() + ", " + allocator + ")",
                    "arena", Arena.class,
                    targetTypeTag, target.typeName(),
                    "interop", ClassNames.INTEROP);

        else
            stmt = PartialStatement.of(
                    "$interop:T.allocateNativeArray(" + identifier
                            + ", " + array.zeroTerminated() + ", " + allocator + ")",
                    "arena", Arena.class,
                    "interop", ClassNames.INTEROP);

        // GArray
        // TODO: when ownership is not transferred, unref the GArray
        if (array.name() != null && "GLib.Array".equals(array.name())) {
            String elemSize = "" + array.anyType().allocatedSize(false);
            if (type.isLong())
                elemSize = "$interop:T.longAsInt() ? 4 : 8";

            return PartialStatement.of(
                    "$interop:T.newGArray(")
                    .add(stmt)
                    .add(", " + identifier + ".length, " + elemSize + ")",
                    "arena", Arena.class,
                    "interop", ClassNames.INTEROP);
        }

        return stmt;
    }

    PartialStatement marshalNativeToJava(String identifier, boolean upcall) {
        if (type != null) {
            if ("gfloat**".equals(type.cType()))
                return PartialStatement.of("null /* unsupported */");

            if (type.isActuallyAnArray())
                return marshalNativeToJavaArray(type, null, identifier);

            if (type.isPointer() && (type.isPrimitive() || target instanceof EnumType))
                return PartialStatement.of(identifier);

            return marshalNativeToJava(type, identifier);
        }

        // String[][]
        if (array != null && array.anyType() instanceof Array inner
                && inner.anyType() instanceof Type t && t.isString())
            return PartialStatement.of(
                    "$interop:T.getStrvArrayFrom(" + identifier + ", " + transfer() + ")",
                    "interop", ClassNames.INTEROP,
                    "transferOwnership", ClassNames.TRANSFER_OWNERSHIP);

        // Array
        if (array != null && array.anyType() instanceof Type t)
             return marshalNativeToJavaArray(t, array.sizeExpression(upcall), identifier);

        // Nested array
        return PartialStatement.of("null /* unsupported */");
    }

    PartialStatement marshalNativeToJava(Type type, String identifier) {
        String targetTypeTag = target == null ? null : type.toTypeTag();
        boolean isTypeInstance = target instanceof Record
                && "TypeInstance".equals(target.name());
        boolean isTypeClass = target instanceof Record
                                    && "TypeClass".equals(target.name());

        if (type.isString())
            return PartialStatement.of(
                    "$interop:T.getStringFrom(" + identifier + ", " + transfer() + ")",
                    "interop", ClassNames.INTEROP,
                    "transferOwnership", ClassNames.TRANSFER_OWNERSHIP);

        if (target instanceof Bitfield bitfield)
            return PartialStatement.of(
                    "$interop:T.intToEnumSet($" + targetTypeTag + ":T.class, "
                            + "$" + targetTypeTag + ":T::of, "
                            + identifier + ")",
                    "interop", ClassNames.INTEROP,
                    targetTypeTag, bitfield.typeName());

        if (target instanceof Enumeration)
            return PartialStatement.of(
                    "$" + targetTypeTag + ":T.of(" + identifier + ")",
                    targetTypeTag, target.typeName());

        // Generate constructor call for GList/GSList with generic element types
        if (target != null && type.checkIsGList()) {
            if (type.anyTypes() == null || type.anyTypes().size() > 1)
                throw new UnsupportedOperationException("Unsupported element type: " + type);

            // Generate lambdas or method references to create and destruct elements
            PartialStatement elementConstructor = getElementConstructor(type, 0);
            PartialStatement elementDestructor = getElementDestructor(type, 0);

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
                case Field _             -> NONE;
                default                  -> throw new IllegalStateException();
            };

            var stmt = PartialStatement.of("new $" + targetTypeTag + ":T(" + identifier + ", ",
                            targetTypeTag, type.typeName())
                    .add(elementConstructor);

            if (elementDestructor != null)
                stmt.add(", ").add(elementDestructor);

            return stmt.add(", $transferOwnership:T." + transferOwnership.toString(),
                            "transferOwnership", ClassNames.TRANSFER_OWNERSHIP)
                    .add(")");
        }

        // Generate constructor call for HashTable with generic types for keys and values
        if (target != null && type.checkIsGHashTable()) {
            if (type.anyTypes() == null || type.anyTypes().size() != 2)
                throw new UnsupportedOperationException("Unsupported element type: " + type);

            PartialStatement keyConstructor = getElementConstructor(type, 0);
            PartialStatement valueConstructor = getElementConstructor(type, 1);

            return PartialStatement.of("new $" + targetTypeTag + ":T(" + identifier + ", ",
                            targetTypeTag, type.typeName())
                    .add(keyConstructor).add(", ").add(valueConstructor).add(")");

        }

        if (target != null && target.checkIsGBytes())
            return PartialStatement.of("$interop:T.fromGBytes(" + identifier + ")",
                    "interop", ClassNames.INTEROP);

        if ((target instanceof Record && !isTypeInstance && !isTypeClass)
                || target instanceof Union
                || target instanceof Boxed
                || (target instanceof Alias a && a.lookup() instanceof Record))
            return PartialStatement.of(
                    "$memorySegment:T.NULL.equals(" + identifier
                            + ") ? null : new $" + targetTypeTag + ":T(" + identifier + ")",
                    "memorySegment", MemorySegment.class,
                    targetTypeTag, target.typeName());

        if (target instanceof Alias a && a.isValueWrapper())
            return PartialStatement.of(
                    "new $" + targetTypeTag + ":T(" + identifier + ")",
                    targetTypeTag, target.typeName());

        if (target instanceof Callback)
            return PartialStatement.of("null /* Unsupported parameter type */");

        boolean hasGType = isTypeInstance
                || target instanceof Class
                || target instanceof Interface
                || (target instanceof Alias a
                    && (a.lookup() instanceof Class || a.lookup() instanceof Interface));

        String cacheFunction = hasGType ? "getForType" : isTypeClass ? "getForTypeClass" : "get";

        if (target instanceof Class
                || target instanceof Interface
                || (target instanceof Alias a && a.isProxy())
                || isTypeInstance
                || isTypeClass)
            return PartialStatement.of(
                        "($" + targetTypeTag + ":T) $instanceCache:T." + cacheFunction + "(" + identifier + ", ")
                    .add(target.constructorName())
                    .add(")",
                            targetTypeTag, target.typeName(),
                            "instanceCache", ClassNames.INSTANCE_CACHE);

        if (type.isBoolean())
            return PartialStatement.of(identifier + " != 0");

        return PartialStatement.of(identifier);
    }

    private static PartialStatement getElementConstructor(Type type, int child) {
        return switch (type.anyTypes().get(child)) {
            case Type t when t.isString() ->
                PartialStatement.of("$interop:T::getStringFrom", "interop", ClassNames.INTEROP);
            case Type t when t.isPrimitive() ->
                PartialStatement.of("$interop:T::get" + primitiveClassName(t.javaType()) + "From",
                        "interop", ClassNames.INTEROP);
            case Type t when t.isMemorySegment() ->
                PartialStatement.of("(_p -> _p)");
            case Array _ ->
                PartialStatement.of("(_p -> _p)");
            case Type t when t.lookup() != null -> {
                var elemTarget = t.lookup();
                // For enum types (bitfield/enumeration) read an integer and call <EnumType>.of()
                if (elemTarget instanceof EnumType enumType) {
                    var elemTypeTag = elemTarget.typeTag();
                    yield PartialStatement.of("(_ptr -> $" + elemTypeTag + ":T.of($interop:T.getIntegerFrom(_ptr)))",
                            elemTypeTag, enumType.typeName(), "interop", ClassNames.INTEROP);
                } else {
                    yield elemTarget.constructorName();
                }
            }
            default ->
                    throw new UnsupportedOperationException("Unsupported element type: " + type);
        };
    }

    private static PartialStatement getElementDestructor(Type type, int child) {
        return switch (type.anyTypes().get(child)) {
            case Array _ ->
                PartialStatement.of("(_ -> {}) /* unsupported */");
            case Type t when t.isString() ->
                null;
            case Type t when t.isPrimitive() ->
                null;
            case Type t when t.isMemorySegment() ->
                PartialStatement.of("$glib:T::free", "glib", ClassNames.G_LIB);
            case Type t when t.lookup() != null ->
                t.lookup().destructorName();
            default ->
                throw new UnsupportedOperationException("Unsupported element type: " + type);
        };
    }

    private PartialStatement marshalNativeToJavaArray(Type type, String size, String identifier) {
        RegisteredType target = type.lookup();
        String targetTypeTag = target != null ? type.toTypeTag() : null;
        String primitive = type.isPrimitive() ? primitiveClassName(type.javaType()) : null;

        // GArray stores the length in the len field
        if (size == null && array != null && array.name() != null
                && List.of("GLib.Array", "GLib.PtrArray", "GLib.ByteArray").contains(array.name())) {
            size = "new $arrayType:T(" + identifier + ").readLen()";
            identifier = "$interop:T.dereference(" + identifier + ")";
        }

        // Null-terminated array
        if (size == null) {
            if ("java.lang.String".equals(type.javaType()))
                return PartialStatement.of(
                        "$interop:T.getStringArrayFrom(" + identifier + ", " + transfer() + ")",
                        "interop", ClassNames.INTEROP,
                        "transferOwnership", ClassNames.TRANSFER_OWNERSHIP);

            if ("java.lang.foreign.MemorySegment".equals(type.javaType()))
                return PartialStatement.of(
                        "$interop:T.getAddressArrayFrom(" + identifier + ", " + transfer() + ")",
                        "interop", ClassNames.INTEROP,
                        "transferOwnership", ClassNames.TRANSFER_OWNERSHIP);

            if (target instanceof EnumType)
                return PartialStatement.of(
                        "$interop:T.getArrayFromIntPointer(" + identifier
                                + ", (int) $" + targetTypeTag + ":T.class, $" + targetTypeTag + ":T::of)",
                        "interop", ClassNames.INTEROP,
                        targetTypeTag, target.typeName());

            if (target instanceof Alias a && a.isValueWrapper())
                return PartialStatement.of(
                        "$" + targetTypeTag + ":T.fromNativeArray(" + identifier + ", " + transfer() + ")",
                        targetTypeTag, target.typeName(),
                        "transferOwnership", ClassNames.TRANSFER_OWNERSHIP);

            if (type.isPrimitive())
                return PartialStatement.of(
                        "$interop:T.get" + primitive + "ArrayFrom(" + identifier + ", _arena, " + transfer() + ")",
                        "interop", ClassNames.INTEROP,
                        "transferOwnership", ClassNames.TRANSFER_OWNERSHIP);

            if (target instanceof Record && (! type.isPointer()) &&
                    (! (array != null && "GLib.PtrArray".equals(array.name()))))
                return PartialStatement.of(
                                "$interop:T.getStructArrayFrom(" + identifier + ", $" + targetTypeTag + ":T.class, ")
                        .add(target.constructorName())
                        .add(", $" + targetTypeTag + ":T.getMemoryLayout())",
                                "interop", ClassNames.INTEROP,
                                targetTypeTag, target.typeName());

            if (target == null)
                throw new IllegalStateException("Target is null for type " + type);

            return PartialStatement.of(
                            "$interop:T.getProxyArrayFrom(" + identifier + ", $" + targetTypeTag + ":T.class, ")
                    .add(target.constructorName())
                    .add(")",
                            "interop", ClassNames.INTEROP,
                            targetTypeTag, target.typeName());
        }

        // Array with known size
        if ("java.lang.String".equals(type.javaType()))
            return PartialStatement.of(
                    "$interop:T.getStringArrayFrom(" + identifier + ", " + size + ", " + transfer() + ")",
                    "interop", ClassNames.INTEROP,
                    "transferOwnership", ClassNames.TRANSFER_OWNERSHIP,
                    "arrayType", array == null ? null : toJavaQualifiedType(array.name(), array.namespace()));

        if ("java.lang.foreign.MemorySegment".equals(type.javaType()))
            return PartialStatement.of(
                    "$interop:T.getAddressArrayFrom(" + identifier + ", " + size + ", " + transfer() + ")",
                    "interop", ClassNames.INTEROP,
                    "transferOwnership", ClassNames.TRANSFER_OWNERSHIP,
                    "arrayType", array == null ? null : toJavaQualifiedType(array.name(), array.namespace()));

        if (target instanceof EnumType)
            return PartialStatement.of(
                    "$interop:T.getArrayFromIntPointer(" + identifier
                            + ", (int) " + size + ", $" + targetTypeTag + ":T.class, $" + targetTypeTag + ":T::of)",
                    "interop", ClassNames.INTEROP,
                    "arrayType", array == null ? null : toJavaQualifiedType(array.name(), array.namespace()),
                    targetTypeTag, target.typeName());

        if (target instanceof Alias a && a.isValueWrapper())
            return PartialStatement.of(
                    "$targetType:T.fromNativeArray(" + identifier + ", " + size + ", " + transfer() + ")",
                    "interop", ClassNames.INTEROP,
                    "transferOwnership", ClassNames.TRANSFER_OWNERSHIP,
                    "targetType", target.typeName(),
                    "arrayType", array == null ? null : toJavaQualifiedType(array.name(), array.namespace()));

        if (type.isPrimitive() && array != null && array.anyType() instanceof Type)
            return PartialStatement.of(
                    "$interop:T.get" + primitive + "ArrayFrom(" + identifier + ", " + size + ", _arena, " + transfer() + ")",
                    "interop", ClassNames.INTEROP,
                    "transferOwnership", ClassNames.TRANSFER_OWNERSHIP,
                    "arrayType", toJavaQualifiedType(array.name(), array.namespace()));

        if (target instanceof Record && (! type.isPointer()) &&
                (! (array != null && "GLib.PtrArray".equals(array.name()))))
            return PartialStatement.of(
                            "$interop:T.getStructArrayFrom(" + identifier + ", (int) " + size + ", $" + targetTypeTag + ":T.class, ")
                    .add(target.constructorName())
                    .add(", $" + targetTypeTag + ":T.getMemoryLayout())",
                            "interop", ClassNames.INTEROP,
                            "arrayType", array == null ? null : toJavaQualifiedType(array.name(), array.namespace()),
                            targetTypeTag, target.typeName());

        if (target == null)
            throw new IllegalStateException("Target is null for type " + type);

        return PartialStatement.of(
                        "$interop:T.getProxyArrayFrom(" + identifier + ", (int) " + size + ", $" + targetTypeTag + ":T.class, ")
                .add(target.constructorName())
                .add(")",
                        "interop", ClassNames.INTEROP,
                        "arrayType", array == null ? null : toJavaQualifiedType(array.name(), array.namespace()),
                        targetTypeTag, target.typeName());
    }

    PartialStatement getGTypeDeclaration() {
        if (array != null) {
            if (array.anyType() instanceof Type arrayType
                    && "utf8".equals(arrayType.name()))
                return PartialStatement.of("$types:T.STRV",
                        "types", ClassNames.TYPES);

            if ("GLib.ByteArray".equals(array.name()))
                return PartialStatement.of("$byteArray:T.getType()", "byteArray", ClassNames.G_BYTE_ARRAY);

            // Other array types are not supported yet, but could be added here
            return PartialStatement.of("$types:T.POINTER /* unsupported */",
                    "types", ClassNames.TYPES);
        }

        if (type == null)
            throw new IllegalStateException("Expected type or array");

        if (type.isPrimitive())
            return PartialStatement.of("$types:T." + switch(type.cType()) {
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
            }, "types", ClassNames.TYPES);

        if (type.isString())
            return PartialStatement.of("$types:T.STRING", "types", ClassNames.TYPES);

        if (type.isMemorySegment())
            return PartialStatement.of("$types:T.POINTER", "types", ClassNames.TYPES);

        if (type.typeName().equals(ClassNames.G_OBJECT))
            return PartialStatement.of("$types:T.OBJECT", "types", ClassNames.TYPES);

        RegisteredType rt = target instanceof Alias a ? a.lookup() : target;

        if (rt != null) {
            if (rt instanceof Class cls && cls.isInstanceOf("GObject", "ParamSpec"))
                return PartialStatement.of("$types:T.PARAM", "types", ClassNames.TYPES);

            if (rt.javaType().equals("org.gnome.glib.Variant"))
                return PartialStatement.of("$types:T.VARIANT", "types", ClassNames.TYPES);

            if (rt.checkIsGBytes())
                return PartialStatement.of("$types:T.BYTES", "types", ClassNames.TYPES);

            if (rt.checkIsGHashTable())
                return PartialStatement.of("$types:T.HASH_TABLE", "types", ClassNames.TYPES);

            if (rt.getTypeFunc() != null) {
                String typeTag = type.toTypeTag();
                return PartialStatement.of("$" + typeTag + ":T.getType()", typeTag, type.typeName());
            }
        }

        if (type.typeName().equals(ClassNames.G_TYPE))
            return PartialStatement.of("$gobjects:T.gtypeGetType()",
                    "gobjects", ClassNames.G_OBJECTS);

        return PartialStatement.of("$types:T.POINTER", "types", ClassNames.TYPES);
    }

    PartialStatement getValueSetter(PartialStatement gTypeDeclaration, String payloadIdentifier) {
        if (array != null) {
            String allocation;

            // GStrv is just an alias for an array of strings, but GByteArray
            // needs to be allocated
            if ("GLib.ByteArray".equals(array.name()))
                allocation = "$byteArray:T.takeUnowned(" + payloadIdentifier + ").handle()";
            else
                allocation = "$interop:T.allocateNativeArray(" + payloadIdentifier + ", true, _arena)";

            return PartialStatement.of(
                    "_value.setBoxed(" + allocation + ")",
                    "byteArray", ClassNames.G_BYTE_ARRAY,
                    "interop", ClassNames.INTEROP);
        }

        // GBytes has its own marshalling function in the Interop class
        if (target != null && target.checkIsGBytes())
            return PartialStatement.of("_value.setBoxed($interop:T.toGBytes(" + payloadIdentifier + "))",
                    "interop", ClassNames.INTEROP);

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
                    String globalClassTag = uncapitalize(rt.namespace().globalClassName());
                    return PartialStatement.of(
                            "$" + globalClassTag + ":T." + setValueFunc + "(_value, " + payloadIdentifier + ")",
                            globalClassTag,
                            toJavaQualifiedType(rt.namespace().globalClassName(), rt.namespace()));
                }
            }
        }

        // Other, known types
        String setValue = switch (gTypeDeclaration.format()) {
            case "$types:T.BOOLEAN" -> "setBoolean";
            case "$types:T.CHAR" -> "setSchar";
            case "$types:T.UCHAR" -> "setUchar";
            case "$types:T.DOUBLE" -> "setDouble";
            case "$types:T.FLOAT" -> "setFloat";
            case "$types:T.INT" -> "setInt";
            case "$types:T.UINT" -> "setUint";
            case "$types:T.LONG" -> "setLong";
            case "$types:T.ULONG" -> "setUlong";
            case "$types:T.INT64" -> "setInt64";
            case "$types:T.UINT64" -> "setUint64";
            case "$types:T.STRING" -> "setString";
            case "$types:T.POINTER" -> "setPointer";
            case "$types:T.PARAM" -> "setParam";
            case "$types:T.VARIANT" -> "setVariant";
            case "$types:T.BOXED", "$types:T.STRV" -> "setBoxed";
            case "$gobjects:T.gtypeGetType()" -> "setGtype";
            default -> type == null ? "UNKNOWN"
                    : target instanceof Enumeration ? "setEnum"
                    : target instanceof Bitfield ? "setFlags"
                    : target instanceof Record ? "setBoxed"
                    : "setObject";
        };

        return switch(setValue) {
            case "setEnum" ->
                    PartialStatement.of("_value" + "." + setValue + "(" + payloadIdentifier + ".getValue())");
            case "setFlags" ->
                    PartialStatement.of("_value" + "." + setValue + "($interop:T.enumSetToInt(" + payloadIdentifier + "))",
                            "interop", ClassNames.INTEROP);
            case "setBoxed", "setPointer" ->
                    PartialStatement.of("_value" + "." + setValue + "(")
                        .add(marshalJavaToNative(payloadIdentifier))
                        .add(")");
            case "setObject" ->
                    PartialStatement.of("_value" + "." + setValue + "(($gobject:T) " + payloadIdentifier + ")",
                            "gobject", ClassNames.G_OBJECT);
            default ->
                    PartialStatement.of("_value" + "." + setValue + "(" + payloadIdentifier + ")");
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
                builder.initializer(marshalNativeToJava(literal(TypeName.INT, value), false).toCodeBlock());
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
    PartialStatement getValueLayout(Type type) {
        String stmt = (type != null && type.isLong())
                ? "($interop:T.longAsInt() ? $valueLayout:T.JAVA_INT : $valueLayout:T.JAVA_LONG)"
                : "$valueLayout:T." + Conversions.getValueLayoutPlain(type, false);
        return PartialStatement.of(stmt,
                "interop", ClassNames.INTEROP,
                "valueLayout", ValueLayout.class);
    }

    // Generate a statement that retrieves a ValueLayout for primitive types,
    // or the MemoryLayout of complex types.
    PartialStatement getMemoryLayout(Type type) {
        PartialStatement valueLayout = getValueLayout(type);
        if (!type.isPrimitive()) {
            var target = type.lookup();
            if (target instanceof StandardLayoutType slt) {
                if (new MemoryLayoutGenerator().canGenerate(slt)) {
                    valueLayout = PartialStatement.of(
                            "$" + target.typeTag() + ":T.getMemoryLayout()",
                            target.typeTag(), target.typeName());
                }
            }
        }
        return valueLayout;
    }
}
