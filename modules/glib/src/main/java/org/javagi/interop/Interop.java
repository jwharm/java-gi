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

package org.javagi.interop;

import java.lang.foreign.*;
import java.lang.invoke.*;
import java.lang.ref.Cleaner;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.gnome.glib.*;
import org.javagi.base.*;

import org.javagi.base.Enumeration;
import org.jetbrains.annotations.Nullable;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.*;
import static org.javagi.base.TransferOwnership.*;

/**
 * The Interop class contains functionality for interoperability with native
 * code.
 */
@SuppressWarnings("unused") // Not all marshaling methods are used currently.
public class Interop {

    private final static int INT_UNBOUNDED = Integer.MAX_VALUE;
    private final static long LONG_UNBOUNDED = Long.MAX_VALUE;

    private final static boolean LONG_AS_INT = Linker.nativeLinker()
            .canonicalLayouts().get("long").equals(JAVA_INT);

    private final static Linker LINKER = Linker.nativeLinker();
    private final static Cleaner CLEANER = Cleaner.create();
    private final static FunctionDescriptor GET_TYPE_FDESC = FunctionDescriptor.of(JAVA_LONG);

    private static SymbolLookup symbolLookup = LINKER.defaultLookup();

    public static boolean longAsInt() {
        return LONG_AS_INT;
    }

    /**
     * Load the specified library using
     * {@link SymbolLookup#libraryLookup(String, Arena)}.
     *
     * @param name the name of the library
     */
    public static synchronized void loadLibrary(String name) {
        symbolLookup = LibLoad.loadLibrary(name, Arena.global())
                .or(Interop.symbolLookup);
    }

    /**
     * Convenience method that calls
     * {@link #downcallHandle(String, FunctionDescriptor, boolean)} with
     * variadic=false.
     *
     * @param  name  name of the native function
     * @param  fdesc function descriptor of the native function
     * @return the MethodHandle
     */
    public static MethodHandle downcallHandle(String name, FunctionDescriptor fdesc) {
        return downcallHandle(name, fdesc, false);
    }

    /**
     * Create a method handle that is used to call the native function with
     * the provided name and function descriptor.
     *
     * @param  name     name of the native function
     * @param  fdesc    function descriptor of the native function
     * @param  variadic whether the function has varargs
     * @return the newly created MethodHandle
     */
    public static MethodHandle downcallHandle(String name, FunctionDescriptor fdesc, boolean variadic) {
        return symbolLookup.find(name).map(addr -> variadic
                ? VarargsInvoker.create(addr, fdesc)
                : LINKER.downcallHandle(addr, fdesc)).orElse(null);
    }

    /**
     * Create a method handle that is used to call the native function at the
     * provided memory address.
     *
     * @param  symbol  memory address of the native function
     * @param  fdesc   function descriptor of the native function
     * @param  options linker options
     * @return the newly created MethodHandle
     * @throws NullPointerException when {@code symbol} or {@code fdesc} is null
     */
    public static MethodHandle downcallHandle(MemorySegment symbol,
                                              FunctionDescriptor fdesc,
                                              Linker.Option... options) {
        if (symbol == null || fdesc == null)
            throw new NullPointerException();
        return LINKER.downcallHandle(symbol, fdesc, options);
    }

    /**
     * Create a method handle for the {@code upcall} method in the provided
     * class.
     *
     * @param  cls        the callback class
     * @param  descriptor the function descriptor for the native function
     * @return a method handle to use when creating an upcall stub
     */
    public static MethodHandle upcallHandle(MethodHandles.Lookup lookup,
                                            Class<?> cls,
                                            FunctionDescriptor descriptor) {
        return upcallHandle(lookup, cls, "upcall", descriptor);
    }

    /**
     * Create a method handle for a method in the provided class.
     *
     * @param  cls        the callback class
     * @param  name       the name of the callback method
     * @param  descriptor the function descriptor for the native function
     * @return a method handle to use when creating an upcall stub
     */
    public static MethodHandle upcallHandle(MethodHandles.Lookup lookup,
                                            Class<?> cls,
                                            String name,
                                            FunctionDescriptor descriptor) {
        try {
            return lookup.findVirtual(cls, name, descriptor.toMethodType());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a SegmentAllocator that uses {@code malloc()} to allocate memory.
     * <p>
     * The returned allocator currently calls {@link GLib#tryMalloc0}, so the
     * allocated memory is zero-initialized, but they may change in the future.
     * <p>
     * It is up to the user of this allocator to release the allocated memory,
     * for example with {@link GLib#free}.
     *
     * @return the newly created SegmentAllocator
     */
    public static SegmentAllocator mallocAllocator() {
        return (byteSize, _) -> {
            if (byteSize < 0)
                throw new IllegalArgumentException("Cannot malloc " + byteSize + " bytes");

            // malloc doesn't allocate 0 bytes, but Java code expects to be
            // able to allocate zero-length memory segments.
            // I can't think of a better solution right now than to allocate
            // 1 byte and reinterpret the returned MemorySegment's size to 0.
            long allocSize = byteSize > 0 ? byteSize : (byteSize + 1);

            MemorySegment segment = GLib.tryMalloc0(allocSize);
            if (segment == null)
                throw new InteropException("malloc of " + byteSize + " bytes failed");
            return segment.reinterpret(byteSize);
        };
    }

    /**
     * Register a Cleaner action that will close the arena when the instance is
     * garbage-collected, coupling the lifetime of the arena to the lifetime of
     * the instance.
     *
     * @param  arena    a memory arena that can be closed (normally
     *                  {@link Arena#ofConfined()} or {@link Arena#ofShared()}.
     * @param  instance an object
     * @return the arena (for method chaining)
     */
    public static Arena attachArena(Arena arena, Object instance) {
        CLEANER.register(instance, arena::close);
        return arena;
    }

    /**
     * Reinterpret {@code address} to {@code newSize} iff {@code newSize} is
     * larger than the current size of {@code address}.
     *
     * @param  address a MemorySegment
     * @param  newSize new size for the MemorySegment
     * @return the same MemorySegment reinterpreted to at least {@code newSize}
     */
    public static MemorySegment reinterpret(MemorySegment address, long newSize) {
        if (address == null || address.byteSize() >= newSize)
            return address;
        else
            return address.reinterpret(newSize);
    }

    /**
     * Dereference a pointer
     *
     * @param  pointer the pointer to dereference
     * @return the value of the pointer
     * @throws NullPointerException when the pointer is null or references null
     */
    public static MemorySegment dereference(MemorySegment pointer) {
        checkNull(pointer);
        return pointer.reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0);
    }

    /**
     * Reinterpret both memory segments to the specified size and copy
     * {@code size} bytes from {@code src} into {@code dst}.
     *
     * @param src source memory segment
     * @param dst destination memory segment
     * @param size the number of bytes to copy
     */
    public static void copy(MemorySegment src, MemorySegment dst, long size) {
        dst.reinterpret(size).copyFrom(src.reinterpret(size));
    }

    /**
     * When {@code pointer} is {@code null}, or {@code pointer} equals
     * {@link MemorySegment#NULL} or reading an address from {@code pointer}
     * returns {@link MemorySegment#NULL}, raise a {@code NullPointerException}.
     *
     * @param pointer the pointer to check
     * @throws NullPointerException when the check failed
     */
    public static void checkNull(MemorySegment pointer) {
        if (pointer == null
                || pointer.equals(NULL)
                || pointer.reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0).equals(NULL))
            throw new NullPointerException("Null pointer: " + pointer);
    }

    /**
     * Get a GType by executing the provided get-type function.
     *
     * @return the gtype from the provided get-type function
     */
    public static Type getType(String getTypeFunction) {
        if (getTypeFunction == null)
            return null;

        try {
            MethodHandle handle = downcallHandle(getTypeFunction, GET_TYPE_FDESC, false);
            if (handle == null)
                return null;
            return new Type((long) handle.invokeExact());
        } catch (Throwable err) {
            throw new AssertionError("Unexpected exception occurred: ", err);
        }
    }

    /**
     * Allocate a native string using
     * {@link SegmentAllocator#allocateFrom(String)}, but return
     * {@link MemorySegment#NULL} for a {@code null} argument.
     *
     * @param  string the string to allocate as a native string (utf8 char*)
     *                (can be {@code null})
     * @param  alloc  the segment allocator to use
     * @return the allocated MemorySegment with the native utf8 string, or
     *         {@link MemorySegment#NULL}
     */
    public static MemorySegment allocateNativeString(String string, SegmentAllocator alloc) {
        return string == null ? NULL : alloc.allocateFrom(string);
    }

    /**
     * Allocate a native string using {@code g_malloc0()}.
     * Will return {@link MemorySegment#NULL} for a {@code null} argument.
     *
     * @param  string the string to allocate as a native string (utf8 char*)
     *                (can be {@code null})
     * @return the allocated MemorySegment with the native utf8 string, that
     *         must be freed with {@code g_free()}, or
     *         {@link MemorySegment#NULL}
     */
    public static MemorySegment allocateUnownedString(String string) {
        if (string == null)
            return NULL;

        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        MemorySegment segment = GLib.malloc0(bytes.length + 1);
        if (segment == null)
            throw new AssertionError("g_malloc() returned null");

        segment = segment.reinterpret(bytes.length + 1);
        MemorySegment.copy(bytes, 0, segment, JAVA_BYTE, 0, bytes.length);
        return segment;
    }

    /**
     * Copy a Java string from native memory using
     * {@code MemorySegment.getString()}. If an error occurs or when the
     * native address is NULL, null is returned.
     * <p>
     * The native memory is not freed.
     *
     * @param  address the memory address of the native String
     *                 (a {@code NULL}-terminated {@code char*})
     * @return a String or null
     */
    public static String getStringFrom(MemorySegment address) {
        return getStringFrom(address, NONE);
    }

    /**
     * Copy a Java string from native memory using
     * {@code MemorySegment.getString()}. If an error occurs or when the
     * native address is NULL, null is returned.
     *
     * @param  address  the memory address of the native String
     *                  (a {@code NULL}-terminated {@code char*})
     * @param  transfer ownership transfer
     * @return a String or null
     */
    public static String getStringFrom(MemorySegment address, TransferOwnership transfer) {
        if (NULL.equals(address))
            return null;

        try {
            return address.reinterpret(LONG_UNBOUNDED).getString(0);
        } finally {
            if (transfer != NONE)
                GLib.free(address);
        }
    }

    /**
     * Copy a boolean value from native memory. If the native address is NULL or
     * contains the value 0, false is returned; else, true is returned.
     *
     * @param  address the memory address of the native boolean (0 is false, any
     *                 other value is true)
     * @return the resulting boolean
     */
    public static boolean getBooleanFrom(MemorySegment address) {
        return getBooleanFrom(address, NONE);
    }

    /**
     * Copy a boolean value from native memory. If the native address is NULL or
     * contains the value 0, false is returned; else, true is returned.
     *
     * @param  address  the memory address of the native boolean (0 is false, any
     *                  other value is true)
     * @param  transfer ownership transfer
     * @return the resulting boolean
     */
    public static boolean getBooleanFrom(MemorySegment address, TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return false;

        try {
            return address.reinterpret(JAVA_INT.byteSize()).get(JAVA_INT, 0) != 0;
        } finally {
            if (transfer != NONE)
                GLib.free(address);
        }
    }

    /**
     * Copy a byte value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address the memory address of the native byte
     * @return the resulting byte
     */
    public static byte getByteFrom(MemorySegment address) {
        return getByteFrom(address, NONE);
    }

    /**
     * Copy a byte value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address  the memory address of the native byte
     * @param  transfer ownership transfer
     * @return the resulting byte
     */
    public static byte getByteFrom(MemorySegment address, TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return 0;

        try {
            return address.reinterpret(JAVA_BYTE.byteSize()).get(JAVA_BYTE, 0);
        } finally {
            if (transfer != NONE)
                GLib.free(address);
        }
    }

    /**
     * Copy a char value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address the memory address of the native char
     * @return the resulting char
     */
    public static char getCharacterFrom(MemorySegment address) {
        return getCharacterFrom(address, NONE);
    }

    /**
     * Copy a char value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address  the memory address of the native char
     * @param  transfer ownership transfer
     * @return the resulting char
     */
    public static char getCharacterFrom(MemorySegment address, TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return 0;

        try {
            return address.reinterpret(JAVA_CHAR.byteSize()).get(JAVA_CHAR, 0);
        } finally {
            if (transfer != NONE)
                GLib.free(address);
        }
    }

    /**
     * Copy a double value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address the memory address of the native double
     * @return the resulting double
     */
    public static double getDoubleFrom(MemorySegment address) {
        return getDoubleFrom(address, NONE);
    }

    /**
     * Copy a double value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address  the memory address of the native double
     * @param  transfer ownership transfer
     * @return the resulting double
     */
    public static double getDoubleFrom(MemorySegment address, TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return 0;

        try {
            return address.reinterpret(JAVA_DOUBLE.byteSize()).get(JAVA_DOUBLE, 0);
        } finally {
            if (transfer != NONE)
                GLib.free(address);
        }
    }

    /**
     * Copy a float value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address the memory address of the native float
     * @return the resulting float
     */
    public static float getFloatFrom(MemorySegment address) {
        return getFloatFrom(address, NONE);
    }

    /**
     * Copy a float value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address  the memory address of the native float
     * @param  transfer ownership transfer
     * @return the resulting float
     */
    public static float getFloatFrom(MemorySegment address, TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return 0;

        try {
            return address.reinterpret(JAVA_FLOAT.byteSize()).get(JAVA_FLOAT, 0);
        } finally {
            if (transfer != NONE)
                GLib.free(address);
        }
    }

    /**
     * Copy an integer value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address the memory address of the native integer
     * @return the resulting integer
     */
    public static int getIntegerFrom(MemorySegment address) {
        return getIntegerFrom(address, NONE);
    }

    /**
     * Copy an integer value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address  the memory address of the native integer
     * @param  transfer ownership transfer
     * @return the resulting integer
     */
    public static int getIntegerFrom(MemorySegment address, TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return 0;

        try {
            return address.reinterpret(JAVA_INT.byteSize()).get(JAVA_INT, 0);
        } finally {
            if (transfer != NONE)
                GLib.free(address);
        }
    }

    /**
     * Copy a long value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address the memory address of the native long
     * @return the resulting long
     */
    public static long getLongFrom(MemorySegment address) {
        return getLongFrom(address, NONE);
    }

    /**
     * Copy a long value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address  the memory address of the native long
     * @param  transfer ownership transfer
     * @return the resulting long
     */
    public static long getLongFrom(MemorySegment address, TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return 0;

        try {
            return address.reinterpret(JAVA_LONG.byteSize()).get(JAVA_LONG, 0);
        } finally {
            if (transfer != NONE)
                GLib.free(address);
        }
    }

    /**
     * Copy a short value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address the memory address of the native short
     * @return the resulting short
     */
    public static short getShortFrom(MemorySegment address) {
        return getShortFrom(address, NONE);
    }

    /**
     * Copy a short value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address  the memory address of the native short
     * @param  transfer ownership transfer
     * @return the resulting short
     */
    public static short getShortFrom(MemorySegment address, TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return 0;

        try {
            return address.reinterpret(JAVA_SHORT.byteSize()).get(JAVA_SHORT, 0);
        } finally {
            if (transfer != NONE)
                GLib.free(address);
        }
    }

    /**
     * Allocate memory for this object. Java-GI must be able to marshal the
     * object, which means it must implement the {@link Proxy} interface, a
     * primitive type, a String, an {@link Enumeration}, a
     * {@code Set<Enumeration>}, or an existing MemorySegment (returned as-is).
     * <p>
     * For most primitive types, the value is written in a newly allocated
     * memory segment. Integers however are returned as an address; for the
     * reason why, read the GLib documentation for the {@code GINT_TO_POINTER}
     * and {@code GPOINTER_TO_INT} macros. The same is done for GTypes
     * (for compatibility with {@code GTYPE_TO_POINTER} and vice versa).
     *
     * @param o the object to allocate memory for
     * @param alloc the memory allocator
     * @return the allocated memory holding the object
     */
    @SuppressWarnings("unchecked") // The cast to Set<Enumeration> is checked
    public static MemorySegment getAddress(Object o, SegmentAllocator alloc) {
        return switch (o) {
            // existing MemorySegment
            case MemorySegment m -> m;
            // string
            case String s    -> alloc.allocateFrom(s);
            // primitive value
            case Boolean b   -> alloc.allocateFrom(JAVA_INT, b ? 1 : 0);
            case Byte b      -> alloc.allocateFrom(JAVA_BYTE, b);
            case Character c -> alloc.allocateFrom(JAVA_CHAR, c);
            case Double d    -> alloc.allocateFrom(JAVA_DOUBLE, d);
            case Float f     -> alloc.allocateFrom(JAVA_FLOAT, f);
            case Integer i   -> MemorySegment.ofAddress((long) i); // GINT_TO_POINTER()
            case Long l      -> longAsInt()
                                    ? alloc.allocateFrom(JAVA_INT, l.intValue())
                                    : alloc.allocateFrom(JAVA_LONG, l);
            case Short s     -> alloc.allocateFrom(JAVA_SHORT, s);
            // proxy instance
            case Proxy p     -> p.handle();
            // gtype
            case Type t      -> MemorySegment.ofAddress(t.getValue()); // GTYPE_TO_POINTER()
            // alias
            case Alias<?> a  -> getAddress(a.getValue(), alloc);
            // enum
            case Enumeration e -> getAddress(e.getValue(), alloc);
            // flags (empty set)
            case Set<?> s when s.isEmpty() -> getAddress(0, alloc);
            // flags
            case Set<?> s when s.iterator().next() instanceof Enumeration ->
                                    getAddress(enumSetToInt((Set<Enumeration>) s), alloc);
            default -> throw new IllegalArgumentException(
                    "Not a MemorySegment, String, primitive, enum/flags or Proxy type");
        };
    }

    /**
     * Read an array of Strings with the requested length from native memory.
     *
     * @param  address  address of the memory segment
     * @param  length   length of the array
     * @param  transfer ownership transfer
     * @return array of Strings
     */
    public static String[] getStringArrayFrom(MemorySegment address,
                                              int length,
                                              TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return null;

        long size = ADDRESS.byteSize();
        MemorySegment array = reinterpret(address, size * length);

        String[] result = new String[length];
        for (int i = 0; i < length; i++) {
            MemorySegment ptr = array.getAtIndex(ADDRESS, i);
            result[i] = getStringFrom(ptr);
            if (transfer == FULL)
                GLib.free(array.getAtIndex(ADDRESS, i));
        }

        if (transfer != NONE)
            GLib.free(array);
        return result;
    }

    /**
     * Read an array of Strings from a {@code NULL}-terminated array in native
     * memory.
     *
     * @param  address  address of the memory segment
     * @param  transfer ownership transfer
     * @return array of Strings
     */
    public static String[] getStringArrayFrom(MemorySegment address,
                                              TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return null;

        MemorySegment array = reinterpret(address, LONG_UNBOUNDED);

        ArrayList<String> result = new ArrayList<>();
        long offset = 0;
        while (true) {
            MemorySegment ptr = array.get(ADDRESS, offset);
            if (NULL.equals(ptr))
                break;
            result.add(getStringFrom(ptr));
            offset += ADDRESS.byteSize();
        }

        if (transfer == FULL)
            GLib.strfreev(array);
        else if (transfer == CONTAINER)
            GLib.free(array);

        return result.toArray(new String[0]);
    }

    /**
     * Read {@code NULL}-terminated arrays of Strings from a
     * {@code NULL}-terminated array in native memory.
     *
     * @param  address  address of the memory segment
     * @param  transfer ownership transfer
     * @return two-dimensional array of Strings
     */
    public static String[][] getStrvArrayFrom(MemorySegment address,
                                              TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return null;

        MemorySegment array = reinterpret(address, LONG_UNBOUNDED);

        ArrayList<String[]> result = new ArrayList<>();
        long offset = 0;
        while (true) {
            MemorySegment ptr = array.get(ADDRESS, offset);
            if (NULL.equals(ptr))
                break;
            result.add(getStringArrayFrom(ptr, transfer == FULL ? FULL : NONE));
            offset += ADDRESS.byteSize();
        }

        if (transfer != NONE)
            GLib.free(address);

        return result.toArray(new String[0][0]);
    }

    /**
     * Read {@code length} arrays of Strings from native memory.
     *
     * @param  address  address of the memory segment
     * @param  length   the length of the array
     * @param  transfer ownership transfer
     * @return two-dimensional array of Strings
     */
    public static String[][] getStrvArrayFrom(MemorySegment address,
                                              int length,
                                              TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return null;

        long size = ADDRESS.byteSize();
        MemorySegment array = reinterpret(address, size * length);

        ArrayList<String[]> result = new ArrayList<>();
        long offset = 0;
        for (int i = 0; i < length; i++) {
            MemorySegment ptr = array.get(ADDRESS, offset);
            result.add(getStringArrayFrom(ptr, transfer == FULL ? FULL : NONE));
            offset += ADDRESS.byteSize();
        }

        if (transfer != NONE)
            GLib.free(address);

        return result.toArray(new String[0][0]);
    }

    /**
     * Read an array of pointers with the requested length from native memory.
     *
     * @param  address  address of the memory segment
     * @param  length   length of the array
     * @param  transfer ownership transfer
     * @return array of pointers
     */
    public static MemorySegment[] getAddressArrayFrom(MemorySegment address,
                                                      int length,
                                                      TransferOwnership transfer) {

        if (address == null || NULL.equals(address))
            return null;

        long size = ADDRESS.byteSize();
        MemorySegment array = reinterpret(address, size * length);

        MemorySegment[] result = new MemorySegment[length];
        for (int i = 0; i < length; i++)
            result[i] = array.getAtIndex(ADDRESS, i);

        if (transfer != NONE)
            GLib.free(address);

        return result;
    }

    /**
     * Read an array of pointers from a {@code NULL}-terminated array in native
     * memory.
     *
     * @param  address  address of the memory segment
     * @param  transfer ownership transfer
     * @return array of pointers
     */
    public static MemorySegment[] getAddressArrayFrom(MemorySegment address,
                                                      TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return null;

        MemorySegment array = reinterpret(address, LONG_UNBOUNDED);

        ArrayList<MemorySegment> result = new ArrayList<>();
        long offset = 0;
        while (true) {
            MemorySegment ptr = array.get(ADDRESS, offset);
            if (NULL.equals(ptr))
                break;
            result.add(ptr);
            offset += ADDRESS.byteSize();
        }

        if (transfer != NONE)
            GLib.free(address);

        return result.toArray(new MemorySegment[0]);
    }

    /**
     * Read an array of booleans with the requested length from native memory.
     * The array is read from native memory as an array of integers with value
     * 1 or 0, and converted to booleans with 1 = true and 0 = false.
     *
     * @param  address  address of the memory segment
     * @param  length   length of the array
     * @param  arena    the memory scope
     * @param  transfer ownership transfer
     * @return array of booleans
     */
    public static boolean[] getBooleanArrayFrom(MemorySegment address,
                                                long length,
                                                Arena arena,
                                                TransferOwnership transfer) {
        int[] intArray = getIntegerArrayFrom(address, length, arena, transfer);
        if (intArray == null)
            return null;

        boolean[] array = new boolean[intArray.length];

        for (int c = 0; c < intArray.length; c++)
            array[c] = (intArray[c] != 0);

        return array;
    }

    /**
     * Read an array of bytes with the requested length from native memory.
     *
     * @param  address  address of the memory segment
     * @param  length   length of the array
     * @param  arena    the memory scope
     * @param  transfer ownership transfer
     * @return array of bytes
     */
    public static byte[] getByteArrayFrom(MemorySegment address,
                                          long length,
                                          Arena arena,
                                          TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return null;

        byte[] array = address.reinterpret(length, arena, null).toArray(JAVA_BYTE);

        if (transfer != NONE)
            GLib.free(address);

        return array;
    }

    /**
     * Read a {@code NULL}-terminated array of bytes from native memory.
     *
     * @param  address  address of the memory segment
     * @param  arena    the memory scope
     * @param  transfer ownership transfer
     * @return array of bytes
     */
    public static byte[] getByteArrayFrom(MemorySegment address,
                                          Arena arena,
                                          TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return null;

        // Find the null byte
        MemorySegment array = address.reinterpret(LONG_UNBOUNDED, arena, null);
        long idx = 0;
        while (array.get(JAVA_BYTE, idx) != 0) {
            idx++;
        }

        return getByteArrayFrom(address, idx, arena, transfer);
    }

    /**
     * Read an array of chars with the requested length from native memory.
     *
     * @param  address  address of the memory segment
     * @param  length   length of the array
     * @param  arena    the memory scope
     * @param  transfer ownership transfer
     * @return array of chars
     */
    public static char[] getCharacterArrayFrom(MemorySegment address,
                                               long length,
                                               Arena arena,
                                               TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return null;

        long size = JAVA_CHAR.byteSize();
        char[] array = address.reinterpret(length * size, arena, null).toArray(JAVA_CHAR);

        if (transfer != NONE)
            GLib.free(address);

        return array;
    }

    /**
     * Read an array of doubles with the requested length from native memory.
     *
     * @param  address  address of the memory segment
     * @param  length   length of the array
     * @param  arena    the memory scope
     * @param  transfer ownership transfer
     * @return array of doubles
     */
    public static double[] getDoubleArrayFrom(MemorySegment address,
                                              long length,
                                              Arena arena,
                                              TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return null;

        long size = JAVA_DOUBLE.byteSize();
        double[] array = address.reinterpret(length * size, arena, null).toArray(JAVA_DOUBLE);

        if (transfer != NONE)
            GLib.free(address);

        return array;
    }

    /**
     * Read an array of floats with the requested length from native memory.
     *
     * @param  address  address of the memory segment
     * @param  length   length of the array
     * @param  arena    the memory scope
     * @param  transfer ownership transfer
     * @return array of floats
     */
    public static float[] getFloatArrayFrom(MemorySegment address,
                                            long length,
                                            Arena arena,
                                            TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return null;

        long size = JAVA_FLOAT.byteSize();
        float[] array = address.reinterpret(length * size, arena, null).toArray(JAVA_FLOAT);

        if (transfer != NONE)
            GLib.free(address);

        return array;
    }

    /**
     * Read a {@code NULL}-terminated array of float from native memory.
     *
     * @param  address  address of the memory segment
     * @param  arena    the memory scope
     * @param  transfer ownership transfer
     * @return array of floats
     */
    public static float[] getFloatArrayFrom(MemorySegment address,
                                            Arena arena,
                                            TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return null;

        // Find the null byte
        MemorySegment array = address.reinterpret(INT_UNBOUNDED, arena, null);
        long idx = 0;
        while (array.getAtIndex(JAVA_FLOAT, idx) != 0) {
            idx++;
        }

        return getFloatArrayFrom(address, idx, arena, transfer);
    }

    /**
     * Read an array of integers with the requested length from native memory.
     *
     * @param  address  address of the memory segment
     * @param  length   length of the array
     * @param  arena    the memory scope
     * @param  transfer ownership transfer
     * @return array of integers
     */
    public static int[] getIntegerArrayFrom(MemorySegment address,
                                            long length,
                                            Arena arena,
                                            TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return null;

        long size = JAVA_INT.byteSize();
        int[] array = address.reinterpret(length * size, arena, null).toArray(JAVA_INT);

        if (transfer != NONE)
            GLib.free(address);

        return array;
    }

    /**
     * Read a {@code NULL}-terminated array of integers from native memory.
     *
     * @param  address  address of the memory segment
     * @param  arena    the memory scope
     * @param  transfer ownership transfer
     * @return array of integers
     */
    public static int[] getIntegerArrayFrom(MemorySegment address,
                                            Arena arena,
                                            TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return null;

        // Find the null byte
        MemorySegment array = address.reinterpret(INT_UNBOUNDED, arena, null);
        long idx = 0;
        while (array.getAtIndex(JAVA_INT, idx) != 0) {
            idx++;
        }

        return getIntegerArrayFrom(address, idx, arena, transfer);
    }

    /**
     * Read an array of longs with the requested length from native memory.
     *
     * @param  address  address of the memory segment
     * @param  length   length of the array
     * @param  arena    the memory scope
     * @param  transfer ownership transfer
     * @return array of longs
     */
    public static long[] getLongArrayFrom(MemorySegment address,
                                          long length,
                                          Arena arena,
                                          TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return null;

        long size = JAVA_LONG.byteSize();
        long[] array = address.reinterpret(length * size, arena, null).toArray(JAVA_LONG);

        if (transfer != NONE)
            GLib.free(address);

        return array;
    }

    /**
     * Read a {@code NULL}-terminated array of longs from native memory.
     *
     * @param  address  address of the memory segment
     * @param  arena    the memory scope
     * @param  transfer ownership transfer
     * @return array of longs
     */
    public static long[] getLongArrayFrom(MemorySegment address,
                                          Arena arena,
                                          TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return null;

        // Find the null byte
        MemorySegment array = address.reinterpret(INT_UNBOUNDED, arena, null);
        long idx = 0;
        while (array.getAtIndex(JAVA_LONG, idx) != 0) {
            idx++;
        }

        return getLongArrayFrom(address, idx, arena, transfer);
    }

    /**
     * Read an array of shorts with the requested length from native memory.
     *
     * @param  address  address of the memory segment
     * @param  length   length of the array
     * @param  arena    the memory scope
     * @param  transfer ownership transfer
     * @return array of shorts
     */
    public static short[] getShortArrayFrom(MemorySegment address,
                                            long length,
                                            Arena arena,
                                            TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return null;

        long size = JAVA_SHORT.byteSize();
        short[] array = address.reinterpret(length * size, arena, null)
                               .toArray(JAVA_SHORT);

        if (transfer != NONE)
            GLib.free(address);

        return array;
    }

    /**
     * Read a {@code NULL}-terminated array of shorts from native memory.
     *
     * @param  address  address of the memory segment
     * @param  arena    the memory scope
     * @param  transfer ownership transfer
     * @return array of shorts
     */
    public static short[] getShortArrayFrom(MemorySegment address,
                                            Arena arena,
                                            TransferOwnership transfer) {
        if (address == null || NULL.equals(address))
            return null;

        // Find the null byte
        MemorySegment array = address.reinterpret(INT_UNBOUNDED, arena, null);
        long idx = 0;
        while (array.getAtIndex(JAVA_SHORT, idx) != 0) {
            idx++;
        }

        return getShortArrayFrom(address, idx, arena, transfer);
    }

    /**
     * Read a {@code NULL}-terminated array of memory addresses from native
     * memory, create a Proxy instance for each address, and return an array of
     * Proxy instances.
     *
     * @param  address address of the memory segment
     * @param  cls     class of the Proxy type
     * @param  make    constructor of the Proxy type
     * @param  <T>     the type of the Proxy instances
     * @return array of Proxy instances
     */
    public static <T extends Proxy>
    T[] getProxyArrayFrom(MemorySegment address,
                          Class<T> cls,
                          Function<MemorySegment, T> make) {
        if (address == null || NULL.equals(address))
            return null;

        MemorySegment array = reinterpret(address, LONG_UNBOUNDED);
        long idx = 0;
        while (!NULL.equals(array.getAtIndex(ADDRESS, idx))) {
            idx++;
        }

        return getProxyArrayFrom(address, (int) idx, cls, make);
    }

    /**
     * Read an array of memory addresses from native memory, create a Proxy
     * instance for each address, and return an array of Proxy instances.
     *
     * @param  address address of the memory segment
     * @param  length  length of the array
     * @param  cls     class of the Proxy type
     * @param  make    constructor of the Proxy type
     * @param  <T>     the type of the Proxy instances
     * @return array of Proxy instances
     */
    public static <T extends Proxy>
    T[] getProxyArrayFrom(MemorySegment address,
                          int length,
                          Class<T> cls,
                          Function<MemorySegment, T> make) {
        if (address == null || NULL.equals(address))
            return null;

        long size = AddressLayout.ADDRESS.byteSize();
        MemorySegment array = reinterpret(address, size * length);

        @SuppressWarnings("unchecked") T[] result = (T[]) Array.newInstance(cls, length);
        for (int i = 0; i < length; i++) {
            result[i] = make.apply(array.getAtIndex(ADDRESS, i));
        }
        return result;
    }

    /**
     * Read a {@code NULL}-terminated array of structs from native memory,
     * create a Proxy instance for each struct, and return an array of Proxy
     * instances. The array must be terminated by a completely null-filled
     * struct.
     *
     * @param  address address of the memory segment
     * @param  cls     class of the Proxy type
     * @param  make    constructor of the Proxy type
     * @param  <T>     the type of the Proxy instances
     * @return array of Proxy instances
     */
    public static <T extends Proxy>
    T[] getStructArrayFrom(MemorySegment address,
                           Class<T> cls,
                           Function<MemorySegment, T> make,
                           MemoryLayout layout) {
        if (address == null || NULL.equals(address))
            return null;

        MemorySegment array = reinterpret(address, LONG_UNBOUNDED);
        long size = layout.byteSize();
        long idx = 0;
        while (!isNullFilled(array, idx * size, size)) {
            idx++;
        }

        return getStructArrayFrom(address, (int) idx, cls, make, layout);
    }

    private static boolean isNullFilled(MemorySegment segment, long offset, long length) {
        for (long i = 0; i < length; i++)
            if (segment.get(JAVA_BYTE, offset + i) != 0)
                return false;
        return true;
    }

    /**
     * Read an array of structs from native memory, create a Proxy instance for
     * each struct, and return an array of Proxy instances.
     *
     * @param  address address of the memory segment
     * @param  length  length of the array
     * @param  cls     class of the Proxy type
     * @param  make    constructor of the Proxy type
     * @param  <T>     the type of the Proxy instances
     * @return array of Proxy instances
     */
    public static <T extends Proxy>
    T[] getStructArrayFrom(MemorySegment address,
                           int length,
                           Class<T> cls,
                           Function<MemorySegment, T> make,
                           MemoryLayout layout) {
        if (address == null || NULL.equals(address))
            return null;

        MemorySegment array = reinterpret(address, layout.byteSize() * length);

        @SuppressWarnings("unchecked") T[] result = (T[]) Array.newInstance(cls, length);
        List<MemorySegment> elements = array.elements(layout).toList();
        for (int i = 0; i < length; i++) {
            result[i] = make.apply(elements.get(i));
        }
        return result;
    }

    /**
     * Read an array of integers from native memory, create a Java instance for
     * each integer value with the provided constructor, and return an array of
     * these instances. This is used to create an array of Enumeration or
     * Bitfield objects from a native integer array.
     *
     * @param  address address of the memory segment
     * @param  length  length of the array
     * @param  cls     class that will be returned in the array
     * @param  make    constructor to create the instances
     * @param  <T>     the type to construct
     * @return array of constructed instances
     */
    public static <T> T[] getArrayFromIntPointer(MemorySegment address,
                                                 int length,
                                                 Class<T> cls,
                                                 Function<Integer, T> make) {
        if (address == null || NULL.equals(address))
            return null;

        long size = AddressLayout.ADDRESS.byteSize();
        MemorySegment array = reinterpret(address, size * length);

        @SuppressWarnings("unchecked") T[] result = (T[]) Array.newInstance(cls, length);
        for (int i = 0; i < length; i++) {
            result[i] = make.apply(array.getAtIndex(JAVA_INT, i));
        }
        return result;
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * strings ({@code NULL}-terminated utf8 {@code char*}).
     *
     * @param  strings        array of Strings
     * @param  zeroTerminated whether to add a {@code NULL} to the array
     * @param  alloc          the segment allocator for the array
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(String[] strings,
                                                    boolean zeroTerminated,
                                                    SegmentAllocator alloc) {
        if (strings == null)
            return NULL;

        int length = zeroTerminated ? strings.length + 1 : strings.length;
        var memorySegment = alloc.allocate(ADDRESS, length);

        for (int i = 0; i < strings.length; i++) {
            var s = strings[i] == null ? NULL : alloc.allocateFrom(strings[i]);
            memorySegment.setAtIndex(ADDRESS, i, s);
        }

        if (zeroTerminated)
            memorySegment.setAtIndex(ADDRESS, strings.length, NULL);

        return memorySegment;
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * arrays of strings (a Strv-array).
     *
     * @param  strvs          the array of String arrays
     * @param  zeroTerminated whether to add a {@code NULL} to the array. The
     *                        embedded arrays are always
     *                        {@code NULL}-terminated.
     * @param  alloc          the segment allocator for the array
     * @param  elementAlloc   the segment allocator for the array elements
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(String[][] strvs,
                                                    boolean zeroTerminated,
                                                    SegmentAllocator alloc,
                                                    SegmentAllocator elementAlloc) {
        if (strvs == null)
            return NULL;

        int length = zeroTerminated ? strvs.length + 1 : strvs.length;
        var memorySegment = alloc.allocate(ADDRESS, length);

        for (int i = 0; i < strvs.length; i++) {
            var s = strvs[i] == null ? NULL
                    : allocateNativeArray(strvs[i], true, elementAlloc);
            memorySegment.setAtIndex(ADDRESS, i, s);
        }

        if (zeroTerminated)
            memorySegment.setAtIndex(ADDRESS, strvs.length, NULL);

        return memorySegment;
    }

    /**
     * Convert a boolean[] array into an int[] array, and calls
     * {@link #allocateNativeArray(int[], boolean, SegmentAllocator)}.
     * Each boolean value "true" is converted 1, boolean value "false" to 0.
     *
     * @param  array          array of booleans
     * @param  zeroTerminated when true, an (int) 0 is appended to the array
     * @param  alloc          the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(boolean[] array,
                                                    boolean zeroTerminated,
                                                    SegmentAllocator alloc) {
        if (array == null)
            return NULL;

        int[] intArray = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            intArray[i] = array[i] ? 1 : 0;
        }

        return allocateNativeArray(intArray, zeroTerminated, alloc);
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * bytes.
     *
     * @param  array          array of bytes
     * @param  zeroTerminated when true, a (byte) 0 is appended to the array
     * @param  alloc          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(byte[] array,
                                                    boolean zeroTerminated,
                                                    SegmentAllocator alloc) {
        if (array == null)
            return NULL;

        byte[] copy = zeroTerminated ?
                Arrays.copyOf(array, array.length + 1)
                : array;

        return alloc.allocateFrom(JAVA_BYTE, copy);
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * chars.
     *
     * @param array          array of chars
     * @param zeroTerminated when true, a (char) 0 is appended to the array
     * @param alloc          the segment allocator for memory allocation
     * @return whe memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(char[] array,
                                                    boolean zeroTerminated,
                                                    SegmentAllocator alloc) {
        if (array == null)
            return NULL;

        char[] copy = zeroTerminated ?
                Arrays.copyOf(array, array.length + 1)
                : array;

        return alloc.allocateFrom(JAVA_CHAR, copy);
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * doubles.
     *
     * @param  array          array of doubles
     * @param  zeroTerminated when true, a (double) 0 is appended to the array
     * @param  alloc          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(double[] array,
                                                    boolean zeroTerminated,
                                                    SegmentAllocator alloc) {
        if (array == null)
            return NULL;

        double[] copy = zeroTerminated ?
                Arrays.copyOf(array, array.length + 1)
                : array;

        return alloc.allocateFrom(JAVA_DOUBLE, copy);
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * floats.
     *
     * @param  array          array of floats
     * @param  zeroTerminated when true, a (float) 0 is appended to the array
     * @param  alloc          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(float[] array,
                                                    boolean zeroTerminated,
                                                    SegmentAllocator alloc) {
        if (array == null)
            return NULL;

        float[] copy = zeroTerminated ?
                Arrays.copyOf(array, array.length + 1)
                : array;

        return alloc.allocateFrom(JAVA_FLOAT, copy);
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * floats.
     *
     * @param  array          array of floats
     * @param  zeroTerminated when true, a (int) 0 is appended to the array
     * @param  alloc          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(int[] array,
                                                    boolean zeroTerminated,
                                                    SegmentAllocator alloc) {
        if (array == null)
            return NULL;

        int[] copy = zeroTerminated ?
                Arrays.copyOf(array, array.length + 1)
                : array;
        return alloc.allocateFrom(JAVA_INT, copy);
    }

    /**
     * Create a new empty GArray.
     *
     * @param  elementSize element size
     * @return the newly created GArray
     */
    public static MemorySegment newGArray(int elementSize) {
        try {
            MemorySegment g_array = (MemorySegment) DowncallHandles.g_array_new.invokeExact(0, 0, elementSize);
            return g_array.reinterpret(org.gnome.glib.Array.getMemoryLayout().byteSize());
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
    }

    /**
     * Create a new GArray with the provided data.
     *
     * @param  data        memory segment containing the array data
     * @param  length      number of array elements
     * @param  elementSize element size
     * @return the newly created GArray
     */
    public static MemorySegment newGArray(MemorySegment data, long length, long elementSize) {
        try {
            MemorySegment g_array = (MemorySegment) DowncallHandles.g_array_new_take.invokeExact(
                    data, length, 0, elementSize);
            return g_array.reinterpret(org.gnome.glib.Array.getMemoryLayout().byteSize());
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
    }

    /**
     * Create a new empty GByteArray.
     *
     * @return the newly create GByteArray
     */
    public static MemorySegment newGByteArray() {
        try (var _arena = Arena.ofConfined()) {
            MemorySegment _result;
            try {
                MemorySegment g_byte_array = (MemorySegment) DowncallHandles.g_byte_array_new.invokeExact();
                return g_byte_array.reinterpret(ByteArray.getMemoryLayout().byteSize());
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
        }
    }

    /**
     * Create a new empty GPtrArray.
     *
     * @return the newly create GPtrArray
     */
    public static MemorySegment newGPtrArray() {
        try (var _arena = Arena.ofConfined()) {
            MemorySegment _result;
            try {
                MemorySegment g_ptr_array = (MemorySegment) DowncallHandles.g_ptr_array_new.invokeExact();
                return g_ptr_array.reinterpret(PtrArray.getMemoryLayout().byteSize());
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
        }
    }

    /**
     * Create a new empty GPtrArray.
     *
     * @return the newly create GPtrArray
     */
    public static MemorySegment newGPtrArray(MemorySegment data, long length) {
        try (var _arena = Arena.ofConfined()) {
            MemorySegment _result;
            try {
                MemorySegment g_ptr_array = (MemorySegment) DowncallHandles.g_ptr_array_new_take.invokeExact(
                        data, length, NULL);
                return g_ptr_array.reinterpret(PtrArray.getMemoryLayout().byteSize());
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
        }
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * longs.
     *
     * @param  array          array of longs
     * @param  zeroTerminated when true, a (long) 0 is appended to the array
     * @param  alloc          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(long[] array,
                                                    boolean zeroTerminated,
                                                    SegmentAllocator alloc) {
        if (array == null)
            return NULL;

        long[] copy = zeroTerminated ?
                Arrays.copyOf(array, array.length + 1)
                : array;

        return alloc.allocateFrom(JAVA_LONG, copy);
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * shorts.
     *
     * @param  array          array of shorts
     * @param  zeroTerminated when true, a (short) 0 is appended to the array
     * @param  alloc          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(short[] array,
                                                    boolean zeroTerminated,
                                                    SegmentAllocator alloc) {
        if (array == null)
            return NULL;

        short[] copy = zeroTerminated ?
                Arrays.copyOf(array, array.length + 1)
                : array;

        return alloc.allocateFrom(JAVA_SHORT, copy);
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * pointers (from Proxy instances).
     *
     * @param  array          array of Proxy instances
     * @param  zeroTerminated whether to add a {@code NULL} to the array
     * @param  alloc          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(Proxy[] array,
                                                    boolean zeroTerminated,
                                                    SegmentAllocator alloc) {
        if (array == null)
            return NULL;

        MemorySegment[] addressArray = new MemorySegment[array.length];
        for (int i = 0; i < array.length; i++) {
            addressArray[i] = array[i] == null ? NULL : array[i].handle();
        }

        return allocateNativeArray(addressArray, zeroTerminated, alloc);
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * structs (from Proxy instances). The actual struct contents (not the
     * pointers) are copied into the array.
     *
     * @param  array          array of Proxy instances
     * @param  layout         the memory layout of the struct
     * @param  zeroTerminated whether to terminate the array by a struct with
     *                        all members being {@code NULL}
     * @param  alloc          the allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(Proxy[] array,
                                                    MemoryLayout layout,
                                                    boolean zeroTerminated,
                                                    SegmentAllocator alloc) {
        if (array == null)
            return NULL;

        long size = layout.byteSize();
        int length = zeroTerminated ? array.length + 1 : array.length;
        MemorySegment segment = alloc.allocate(layout, length);

        for (int i = 0; i < array.length; i++) {
            if (array[i] != null && (!NULL.equals(array[i].handle()))) {
                // Copy array element to the native array
                segment.asSlice(i * layout.byteSize()).copyFrom(array[i].handle());
            } else {
                // Fill the array slice with zeros
                segment.asSlice(i * size, size).fill((byte) 0);
            }
        }

        if (zeroTerminated)
            // The array is zero-terminated by a struct with all members being 0
            segment.asSlice(array.length * size, size).fill((byte) 0);

        return segment;
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * memory addresses.
     *
     * @param  array          array of MemorySegments
     * @param  zeroTerminated whether to add a {@code NULL} to the array
     * @param  alloc          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(MemorySegment[] array,
                                                    boolean zeroTerminated,
                                                    SegmentAllocator alloc) {

        if (array == null)
            return NULL;

        int length = zeroTerminated ? array.length + 1 : array.length;
        var memorySegment = alloc.allocate(ADDRESS, length);

        for (int i = 0; i < array.length; i++) {
            MemorySegment s = array[i] == null ? NULL : array[i];
            memorySegment.setAtIndex(ADDRESS, i, s);
        }

        if (zeroTerminated)
            memorySegment.setAtIndex(ADDRESS, array.length, NULL);

        return memorySegment;
    }

    /**
     * Create an EnumSet of class `cls` from the provided bitfield.
     * Undefined flags are logged (with level
     * {@link LogLevelFlags#LEVEL_WARNING}) and ignored.
     *
     * @param  <T>      an enum implementing the Java-GI Enumeration interface
     * @param  cls      the class of the enum
     * @param  make     function that will construct an enum from one flag value
     * @param  bitfield the integer containing the bitfield
     * @return an EnumSet containing the enum values as set in the bitfield
     */
    public static <T extends Enum<T> & Enumeration>
    EnumSet<T> intToEnumSet(Class<T> cls, Function<Integer, T> make, int bitfield) {
        int n = bitfield;
        EnumSet<T> enumSet = EnumSet.noneOf(cls);
        int position = 0;
        while (n != 0) {
            if ((n & 1) == 1) {
                // Gracefully handle undefined flags
                try {
                    T flag = make.apply(1 << position);
                    enumSet.add(flag);
                } catch (IllegalStateException e) {
                    GLib.log(Constants.LOG_DOMAIN,
                             LogLevelFlags.LEVEL_DEBUG,
                             "Unexpected flag %d in enum %s\n",
                             n,
                             cls == null ? "NULL" : cls.getName());
                }
            }
            position++;
            n >>= 1;
        }
        return enumSet;
    }

    /**
     * Create a bitfield from the provided Set of enums
     *
     * @param  set the set of enums
     * @return the resulting bitfield
     */
    public static int enumSetToInt(Set<? extends Enumeration> set) {
        int bitfield = 0;
        if (set != null)
            for (Enumeration element : set)
                bitfield |= element.getValue();
        return bitfield;
    }

    /**
     * Convert an array of enums into an array of integers.
     *
     * @param  array an array of enums
     * @return an array containing the integer values of the provided
     *         Enumeration instances
     */
    public static int[] getValues(Enumeration[] array) {
        if (array == null)
            return null;
        int[] values = new int[array.length];
        for (int i = 0; i < array.length; i++)
            values[i] = array[i].getValue();
        return values;
    }

    /**
     * Create a GBytes from a Java byte array
     *
     * @param  data the Java byte array
     * @return the GBytes
     */
    public static MemorySegment toGBytes(byte[] data) {
        try (var _arena = Arena.ofConfined()) {
            long size = data == null ? 0L : data.length;
            MemorySegment _result;
            try {
                _result = (MemorySegment) DowncallHandles.g_bytes_new.invokeExact(
                        (MemorySegment) (data == null ? NULL : allocateNativeArray(data, false, _arena)),
                        size);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
            return _result;
        }
    }

    /**
     * Create a Java byte array from a GBytes
     *
     * @param  address the memory address of the GBytes
     * @return the Java byte array
     */
    public static byte[] fromGBytes(MemorySegment address) {
        try (var _arena = Arena.ofConfined()) {
            MemorySegment _sizePointer = _arena.allocate(JAVA_LONG);
            _sizePointer.set(JAVA_LONG, 0L, 0L);
            Out<Long> size = new Out<>();
            MemorySegment _result;
            try {
                _result = (MemorySegment) DowncallHandles.g_bytes_get_data.invokeExact(address, _sizePointer);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
            size.set(_sizePointer.get(JAVA_LONG, 0));
            return getByteArrayFrom(_result, size.get().intValue(), _arena, NONE);
        }
    }

    /**
     * Free a GBytes with {@code g_bytes_unref()}
     *
     * @param address the address of the GBytes to free
     */
    public static void freeGBytes(MemorySegment address) {
        try {
            DowncallHandles.g_bytes_unref.invokeExact(address);
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
    }

    /**
     * Marshal a GString to a Java String.
     *
     * @param  address  address of a GString
     * @param  transfer if not NONE, the GString is freed
     * @return the Java String
     */
    public static String fromGString(MemorySegment address, TransferOwnership transfer) {
        // This works, because str is the first member of the GString struct,
        // and is guaranteed to be nul-terminated.
        String string = Interop.getStringFrom(address);

        if (transfer != NONE) {
            // Free the GString (including the character data).
            freeGString(address);
        }

        return string;
    }

    /**
     * Marshal a Java String to a GString.
     *
     * @param  string a Java String
     * @return the address of the GString
     */
    public static MemorySegment toGString(String string) {
        MemorySegment allocatedString = Interop.allocateUnownedString(string);
        MemorySegment result;
        try {
            result = (MemorySegment) DowncallHandles.g_string_new_take.invokeExact(allocatedString);
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        return result;
    }

    /**
     * Free a GString (including the character data).
     *
     * @param address address of a GString
     */
    public static void freeGString(MemorySegment address) {
        try {
            MemorySegment ignored = (MemorySegment) DowncallHandles.g_string_free.invokeExact(address, 1);
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
    }

    /**
     * Null-safe retrieve the value of a Boolean Out
     *
     * @param  val a possibly null {@code Out<Boolean>}
     * @return the boolean value, or false when null
     */
    public static boolean toBoolean(@Nullable Out<@Nullable Boolean> val) {
        return val != null && Boolean.TRUE.equals(val.get());
    }

    /**
     * Null-safe retrieve the value of a Byte Out
     *
     * @param  val a possibly null {@code Out<Byte>}
     * @return the byte value, or 0 when null
     */
    public static byte toByte(@Nullable Out<@Nullable Byte> val) {
        if (val != null) {
            Byte value = val.get();
            if (value != null)
                return value;
        }
        return (byte) 0;
    }

    /**
     * Null-safe retrieve the value of a Character Out
     *
     * @param  val a possibly null {@code Out<Character>}
     * @return the char value, or 0 when null
     */
    public static char toCharacter(@Nullable Out<@Nullable Character> val) {
        if (val != null) {
            Character value = val.get();
            if (value != null)
                return value;
        }
        return (char) 0;
    }

    /**
     * Null-safe retrieve the value of a Double Out
     *
     * @param  val a possibly null {@code Out<Double>}
     * @return the double value, or 0 when null
     */
    public static double toDouble(@Nullable Out<@Nullable Double> val) {
        if (val != null) {
            Double value = val.get();
            if (value != null)
                return value;
        }
        return 0.0d;
    }

    /**
     * Null-safe retrieve the value of a Float Out
     *
     * @param  val a possibly null {@code Out<Float>}
     * @return the float value, or 0 when null
     */
    public static float toFloat(@Nullable Out<@Nullable Float> val) {
        if (val != null) {
            Float value = val.get();
            if (value != null)
                return value;
        }
        return 0.0f;
    }

    /**
     * Null-safe retrieve the value of an Integer Out
     *
     * @param  val a possibly null {@code Out<Integer>}
     * @return the int value, or 0 when null
     */
    public static int toInteger(@Nullable Out<@Nullable Integer> val) {
        if (val != null) {
            Integer value = val.get();
            if (value != null)
                return value;
        }
        return 0;
    }

    /**
     * Null-safe retrieve the value of a Long Out
     *
     * @param  val a possibly null {@code Out<Long>}
     * @return the long value, or 0 when null
     */
    public static long toLong(@Nullable Out<@Nullable Long> val) {
        if (val != null) {
            Long value = val.get();
            if (value != null)
                return value;
        }
        return 0L;
    }

    /**
     * Null-safe retrieve the value of a Short Out
     *
     * @param  val a possibly null {@code Out<Short>}
     * @return the short value, or 0 when null
     */
    public static short toShort(@Nullable Out<@Nullable Short> val) {
        if (val != null) {
            Short value = val.get();
            if (value != null)
                return value;
        }
        return (short) 0;
    }

    private static class DowncallHandles {
        static {
            GLib.javagi$ensureInitialized();
        }

        private static final MethodHandle g_bytes_new = Interop.downcallHandle(
                "g_bytes_new",
                FunctionDescriptor.of(ADDRESS, ADDRESS,JAVA_LONG),
                false);

        private static final MethodHandle g_bytes_get_data = Interop.downcallHandle(
                "g_bytes_get_data",
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS),
                false);

        private static final MethodHandle g_bytes_unref = Interop.downcallHandle(
                "g_bytes_unref",
                FunctionDescriptor.ofVoid(ADDRESS),
                false);

        private static final MethodHandle g_array_new = Interop.downcallHandle(
                "g_array_new",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_INT, ValueLayout.JAVA_INT),
                false);

        private static final MethodHandle g_array_new_take = Interop.downcallHandle(
                "g_array_new_take",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG),
                false);

        private static final MethodHandle g_byte_array_new = Interop.downcallHandle(
                "g_byte_array_new",
                FunctionDescriptor.of(ValueLayout.ADDRESS),
                false);

        private static final MethodHandle g_ptr_array_new = Interop.downcallHandle(
                "g_ptr_array_new",
                FunctionDescriptor.of(ValueLayout.ADDRESS),
                false);

        private static final MethodHandle g_ptr_array_new_take = Interop.downcallHandle(
                "g_ptr_array_new_take",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                false);

        private static final MethodHandle g_string_new_take = Interop.downcallHandle(
                "g_string_new_take",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                false);

        private static final MethodHandle g_string_free = Interop.downcallHandle(
                "g_string_free",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.JAVA_INT),
                false);
    }
}
