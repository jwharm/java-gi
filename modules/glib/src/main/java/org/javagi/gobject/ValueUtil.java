/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 Jan-Willem Harmannij
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

package org.javagi.gobject;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Set;
import java.util.function.Function;

import org.gnome.glib.Variant;
import org.javagi.base.Proxy;
import org.gnome.glib.ByteArray;
import org.gnome.glib.Type;
import org.gnome.gobject.*;

import org.javagi.base.Enumeration;
import org.javagi.base.TransferOwnership;
import org.javagi.gobject.types.TypeCache;
import org.javagi.interop.Interop;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.javagi.gobject.types.Types.*;
import static org.gnome.gobject.GObjects.gtypeGetType;
import static org.gnome.gobject.GObjects.typeIsA;

/**
 * Utility functions to convert a {@link Value} to and from a Java Object.
 */
public class ValueUtil {
    
    /**
     * Read the GType from the GValue, call the corresponding getter (using the
     * methods defined in the {@link Value} proxy class), and return the result.
     *
     * @param  src a GValue instance.
     * @return     a Java object (or boxed primitive value) that has been
     *             marshaled from the GValue, or {@code null} if {@code src} is
     *             null.
     */
    public static @Nullable Object valueToObject(@Nullable Value src) {
        if (src == null)
            return null;

        Type type = src.readGType();
        if (type == null || type.equals(NONE))
            return null;

        // Fundamental types
        if (type.equals(BOOLEAN))        return src.getBoolean();
        if (type.equals(CHAR))           return (char) src.getSchar();
        if (type.equals(UCHAR))          return (char) src.getUchar();
        if (type.equals(DOUBLE))         return src.getDouble();
        if (type.equals(FLOAT))          return src.getFloat();
        if (type.equals(INT))            return src.getInt();
        if (type.equals(UINT))           return src.getUint();
        if (type.equals(LONG))           return src.getLong();
        if (type.equals(ULONG))          return src.getUlong();
        if (type.equals(INT64))          return src.getInt64();
        if (type.equals(STRING))         return src.getString();
        if (type.equals(POINTER))        return src.getPointer();
        if (type.equals(PARAM))          return src.getParam();
        if (type.equals(VARIANT))        return src.getVariant();

        // GType
        if (type.equals(gtypeGetType())) return src.getGtype();

        // GObject
        if (typeIsA(type, OBJECT))       return src.getObject();

        // GStrv
        if (type.equals(STRV))
            return Interop.getStringArrayFrom(src.getBoxed(), TransferOwnership.NONE);

        // GByteArray
        if (type.equals(BYTE_ARRAY)) {
            MemorySegment address = src.getBoxed();
            ByteArray arr = new ByteArray(address);
            MemorySegment data = Interop.dereference(address);
            int length = arr.readLen();
            try (var arena = Arena.ofConfined()) {
                return Interop.getByteArrayFrom(data, length, arena, TransferOwnership.NONE);
            }
        }

        // Boxed type
        if (BoxedUtil.isBoxed(type)) {
            MemorySegment address = src.getBoxed();
            var ctor = TypeCache.getConstructor(type, null);
            if (ctor == null)
                throw new UnsupportedOperationException("Unsupported boxed type: " + GObjects.typeName(type));
            return ctor.apply(address);
        }

        // Enum
        if (typeIsA(type, ENUM)) {
            int value = src.getEnum();
            var ctor = TypeCache.getEnumConstructor(type);
            if (ctor == null)
                throw new UnsupportedOperationException("Unsupported enum type: " + GObjects.typeName(type));
            return ctor.apply(value);
        }

        // Flags
        if (typeIsA(type, FLAGS)) {
            int value = src.getFlags();
            return intToFlags(value, type);
        }

        throw new UnsupportedOperationException("Unsupported type: " + GObjects.typeName(type));
    }

    /**
     * Use the TypeCache to create a set of Java enum values for the provided bitfield (int).
     *
     * @param value the bitfield
     * @param type  the GType of the Flags type
     * @param <T>   the Flags type
     * @return an EnumSet of Java enum values
     */
    private static <T extends Enum<T> & Enumeration> Object intToFlags(int value, Type type) {
        Function<Integer, T> ctor = TypeCache.getEnumConstructor(type);
        if (ctor == null)
            throw new UnsupportedOperationException("Unsupported enum type: " + GObjects.typeName(type));
        var cls = getEnumClass(ctor, type);
        return Interop.intToEnumSet(cls, ctor, value);
    }

    /**
     * Get the Java class of {@code T}.
     * <p>
     * We need to know the Java class of flags types, but all we have is a
     * constructor function (usually {@code ClassName#of(int)}). So we apply
     * the constructor for a random value and call {@code getClass()} on the
     * result. Ideally, there should be a 0 member (i.e. no flags set) but that
     * is not always the case. So we try all 32 bits, until a valid one is
     * found.
     *
     * @param ctor the constructor that will be run
     * @param type the GType, only used for logging purposes
     * @param <T> the flags type
     * @return the Java class of T
     */
    private static <T> Class<T> getEnumClass(Function<Integer, T> ctor, Type type) {
        for (int i = 0; i < 32; i++) {
            try {
                T instance = ctor.apply(2^i);
                // If this cast doesn't succeed, someone is apparently creating
                // hierarchies of flags types, which is entirely unsupported
                @SuppressWarnings("unchecked")
                Class<T> cls = (Class<T>) instance.getClass();
                return cls;
            } catch (IllegalStateException ignored) {
            }
        }
        throw new UnsupportedOperationException("Cannot get Java class for " + GObjects.typeName(type));
    }

    /**
     * Read the GType of the {@code dest} GValue and set the {@code src} object
     * (or boxed primitive value) as its value using the corresponding setter in
     * the {@link Value} proxy class.
     *
     * @param  src  the Java Object (or boxed primitive value) to put in the
     *              GValue. Should not be {@code null}
     * @param  dest the GValue to write to. Should not be {@code null}
     * @return {@code true} if the value was set, and {@code false} otherwise.
     */
    @SuppressWarnings({"unchecked", "rawtypes"}) // Flags parameters are always Set<Enum>
    public static boolean objectToValue(@Nullable Object src, @Nullable Value dest) {
        if (src == null || dest == null)
            return false;

        Type type = dest.readGType();
        if (type == null)
            return false;

        if      (type.equals(BOOLEAN))        dest.setBoolean((Boolean) src);
        else if (type.equals(CHAR))           dest.setSchar((byte) ((Character) src).charValue());
        else if (type.equals(UCHAR))          dest.setUchar((byte) ((Character) src).charValue());
        else if (type.equals(DOUBLE))         dest.setDouble((Double) src);
        else if (type.equals(FLOAT))          dest.setFloat((Float) src);
        else if (type.equals(INT))            dest.setInt((Integer) src);
        else if (type.equals(UINT))           dest.setUint((Integer) src);
        else if (type.equals(LONG))           dest.setLong(toInt(src));
        else if (type.equals(ULONG))          dest.setUlong(toInt(src));
        else if (type.equals(INT64))          dest.setInt64((Long) src);
        else if (type.equals(STRING))         dest.setString((String) src);
        else if (type.equals(ENUM))           dest.setEnum(((Enumeration) src).getValue());
        else if (type.equals(FLAGS))          dest.setFlags(Interop.enumSetToInt((Set) src));
        else if (type.equals(OBJECT))         dest.setObject((GObject) src);
        else if (type.equals(gtypeGetType())) dest.setGtype((Type) src);
        else if (type.equals(POINTER))        dest.setPointer((MemorySegment) src);
        else if (type.equals(PARAM))          dest.setParam((ParamSpec) src);
        else if (type.equals(STRV))           dest.setBoxed(Interop.allocateNativeArray((String[]) src, true, Interop.mallocAllocator()));
        else if (type.equals(BYTE_ARRAY))     dest.setBoxed(ByteArray.take((byte[]) src).handle());
        else if (type.equals(VARIANT))        dest.setVariant((Variant) src);
        else if (typeIsA(type, OBJECT))       dest.setObject((GObject) src);
        else if (typeIsA(type, ENUM))         dest.setEnum(((Enumeration) src).getValue());
        else if (typeIsA(type, FLAGS))        dest.setFlags(flagsToInt(type, src));
        else if (BoxedUtil.isBoxed(type))     dest.setBoxed(((Proxy) src).handle());
        else throw new UnsupportedOperationException("Unsupported type: " + GObjects.typeName(type));

        return true;
    }

    /**
     * Convert a flags parameter to a bitfield (int).
     *
     * @param type the GType of the flags type
     * @param src  either a single enum value, or a Set of enum values
     * @param <T>  the flags type
     * @return the bitfield (int) value
     */
    private static <T extends Enum<T> & Enumeration> int flagsToInt(Type type, Object src) {
        if (src instanceof Enumeration e)
            return e.getValue();

        @SuppressWarnings("unchecked") // throw ClassCastException when src is not a Set
        Set<T> set = (Set<T>) src;
        return Interop.enumSetToInt(set);
    }

    /*
     * On Linux and macOS, Long values are 64 bit in native code. On Windows,
     * Long values are 32 bit. To preserve cross-platform compatibility, Java-GI
     * converts all Java Long values to Integers.
     */
    private static int toInt(@NotNull Object src) {
        return src instanceof Long l ? l.intValue() : (Integer) src;
    }

    /**
     * Allocate and initialize a new GValue, and copy {@code src} into it.
     *
     * @param src the GValue to copy
     * @return the newly created GValue, or {@code null} if {@code src} is null
     */
    public static @Nullable Value copy(@Nullable Value src) {
        if (src == null)
            return null;

        Value dest = new Value();
        dest.init(src.readGType());
        src.copy(dest);
        return dest;
    }
}
