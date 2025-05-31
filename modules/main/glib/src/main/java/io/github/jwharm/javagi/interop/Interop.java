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

package io.github.jwharm.javagi.interop;

import java.lang.foreign.*;
import java.lang.invoke.*;
import java.lang.ref.Cleaner;
import java.lang.reflect.Array;
import java.util.*;
import java.util.Arrays;
import java.util.function.Function;

import io.github.jwharm.javagi.Constants;
import io.github.jwharm.javagi.base.Enumeration;
import org.gnome.glib.GLib;

import io.github.jwharm.javagi.base.*;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.jetbrains.annotations.Nullable;

import static java.lang.Long.max;
import static java.lang.foreign.MemorySegment.NULL;

/**
 * The Interop class contains functionality for interoperability with native
 * code.
 */
@SuppressWarnings("unused") // Not all marshaling methods are used currently.
public class Interop {

    private final static int INT_UNBOUNDED = Integer.MAX_VALUE;

    private final static long LONG_UNBOUNDED = Long.MAX_VALUE;

    private final static boolean LONG_AS_INT = Linker.nativeLinker()
            .canonicalLayouts().get("long").equals(ValueLayout.JAVA_INT);

    private final static Linker LINKER = Linker.nativeLinker();

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
    public static MethodHandle downcallHandle(String name,
                                              FunctionDescriptor fdesc) {
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
    public static MethodHandle downcallHandle(String name,
                                              FunctionDescriptor fdesc,
                                              boolean variadic) {
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
     */
    public static MethodHandle downcallHandle(MemorySegment symbol,
                                              FunctionDescriptor fdesc,
                                              Linker.Option... options) {
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
     * Register a Cleaner that will close the arena when the instance is
     * garbage-collected, coupling the lifetime of the arena to the lifetime of
     * the instance.
     *
     * @param  arena    a memory arena that can be closed (normally
     *                  {@link Arena#ofConfined()} or {@link Arena#ofAuto()}.
     * @param  instance an object
     * @return the arena (for method chaining)
     */
    public static Arena attachArena(Arena arena, Object instance) {
        Cleaner cleaner = Cleaner.create();
        cleaner.register(instance, arena::close);
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
    public static MemorySegment reinterpret(MemorySegment address,
                                            long newSize) {
        if (address == null || address.byteSize() >= newSize)
            return address;

        return address.reinterpret(newSize);
    }

    /**
     * Dereference a pointer
     *
     * @param  pointer the pointer to dereference
     * @return the value of the pointer, or {@code null} in case of a pointer to
     *         {@code NULL}.
     */
    public static MemorySegment dereference(MemorySegment pointer) {
        if (pointer == null || NULL.equals(pointer))
            return NULL;

        return pointer
                .reinterpret(ValueLayout.ADDRESS.byteSize())
                .get(ValueLayout.ADDRESS, 0);
    }

    /**
     * First reinterpret the memory segments so they have equal size, then copy
     * {@code src} into {@code dst}.
     *
     * @param src source memory segment
     * @param dst destination memory segment
     */
    public static void copy(MemorySegment src, MemorySegment dst) {
        long size = max(src.byteSize(), dst.byteSize());
        src.reinterpret(size);
        dst.reinterpret(size);
        dst.copyFrom(src);
    }

    /**
     * Get a GType by executing the provided get-type function.
     *
     * @return the gtype from the provided get-type function
     */
    public static Type getType(String getTypeFunction) {
        if (getTypeFunction == null)
            return null;

        FunctionDescriptor fdesc = FunctionDescriptor.of(ValueLayout.JAVA_LONG);

        try {
            MethodHandle handle = downcallHandle(getTypeFunction, fdesc, false);
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
     * @param  string    the string to allocate as a native string (utf8 char*)
     *                   (can be {@code null})
     * @param  alloc the segment allocator to use
     * @return the allocated MemorySegment with the native utf8 string, or
     *         {@link MemorySegment#NULL}
     */
    public static MemorySegment allocateNativeString(String string,
                                                     SegmentAllocator alloc) {
        return string == null ? NULL : alloc.allocateFrom(string);
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
        return getStringFrom(address, false);
    }

    /**
     * Copy a Java string from native memory using
     * {@code MemorySegment.getString()}. If an error occurs or when the
     * native address is NULL, null is returned.
     *
     * @param  address the memory address of the native String
     *                 (a {@code NULL}-terminated {@code char*})
     * @param  free    if the address must be freed
     * @return a String or null
     */
    public static String getStringFrom(MemorySegment address, boolean free) {
        if (NULL.equals(address))
            return null;

        try {
            return address.reinterpret(LONG_UNBOUNDED).getString(0);
        } finally {
            if (free)
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
        return getBooleanFrom(address, false);
    }

    /**
     * Copy a boolean value from native memory. If the native address is NULL or
     * contains the value 0, false is returned; else, true is returned.
     *
     * @param  address the memory address of the native boolean (0 is false, any
     *                 other value is true)
     * @param  free    if the address must be freed
     * @return the resulting boolean
     */
    public static boolean getBooleanFrom(MemorySegment address, boolean free) {
        if (NULL.equals(address))
            return false;

        try {
            return address.reinterpret(LONG_UNBOUNDED)
                    .get(ValueLayout.JAVA_INT, 0) != 0;
        } finally {
            if (free)
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
        return getByteFrom(address, false);
    }

    /**
     * Copy a byte value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address the memory address of the native byte
     * @param  free    if the address must be freed
     * @return the resulting byte
     */
    public static byte getByteFrom(MemorySegment address, boolean free) {
        if (NULL.equals(address))
            return 0;

        try {
            return address.reinterpret(LONG_UNBOUNDED)
                    .get(ValueLayout.JAVA_BYTE, 0);
        } finally {
            if (free)
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
        return getCharacterFrom(address, false);
    }

    /**
     * Copy a char value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address the memory address of the native char
     * @param  free    if the address must be freed
     * @return the resulting char
     */
    public static char getCharacterFrom(MemorySegment address, boolean free) {
        if (NULL.equals(address))
            return 0;

        try {
            return address.reinterpret(LONG_UNBOUNDED)
                    .get(ValueLayout.JAVA_CHAR, 0);
        } finally {
            if (free)
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
        return getDoubleFrom(address, false);
    }

    /**
     * Copy a double value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address the memory address of the native double
     * @param  free    if the address must be freed
     * @return the resulting double
     */
    public static double getDoubleFrom(MemorySegment address, boolean free) {
        if (NULL.equals(address))
            return 0;

        try {
            return address.reinterpret(LONG_UNBOUNDED)
                    .get(ValueLayout.JAVA_DOUBLE, 0);
        } finally {
            if (free)
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
        return getFloatFrom(address, false);
    }

    /**
     * Copy a float value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address the memory address of the native float
     * @param  free    if the address must be freed
     * @return the resulting float
     */
    public static float getFloatFrom(MemorySegment address, boolean free) {
        if (NULL.equals(address))
            return 0;

        try {
            return address.reinterpret(LONG_UNBOUNDED)
                    .get(ValueLayout.JAVA_FLOAT, 0);
        } finally {
            if (free)
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
        return getIntegerFrom(address, false);
    }

    /**
     * Copy an integer value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address the memory address of the native integer
     * @param  free    if the address must be freed
     * @return the resulting integer
     */
    public static int getIntegerFrom(MemorySegment address, boolean free) {
        if (NULL.equals(address))
            return 0;

        try {
            return address.reinterpret(LONG_UNBOUNDED)
                          .get(ValueLayout.JAVA_INT, 0);
        } finally {
            if (free)
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
        return getLongFrom(address, false);
    }

    /**
     * Copy a long value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address the memory address of the native long
     * @param  free    if the address must be freed
     * @return the resulting long
     */
    public static long getLongFrom(MemorySegment address, boolean free) {
        if (NULL.equals(address))
            return 0;

        try {
            return address.reinterpret(LONG_UNBOUNDED)
                          .get(ValueLayout.JAVA_LONG, 0);
        } finally {
            if (free)
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
        return getShortFrom(address, false);
    }

    /**
     * Copy a short value from native memory. If the native address is NULL,
     * 0 is returned.
     *
     * @param  address the memory address of the native short
     * @param  free    if the address must be freed
     * @return the resulting short
     */
    public static short getShortFrom(MemorySegment address, boolean free) {
        if (NULL.equals(address))
            return 0;

        try {
            return address.reinterpret(LONG_UNBOUNDED)
                    .get(ValueLayout.JAVA_SHORT, 0);
        } finally {
            if (free)
                GLib.free(address);
        }
    }

    public static MemorySegment getAddress(Object o, Arena arena) {
        return switch (o) {
            case MemorySegment m -> m;
            case String s    -> arena.allocateFrom(s);
            case Boolean b   -> arena.allocateFrom(ValueLayout.JAVA_INT, b ? 1 : 0);
            case Byte b      -> arena.allocateFrom(ValueLayout.JAVA_BYTE, b);
            case Character c -> arena.allocateFrom(ValueLayout.JAVA_CHAR, c);
            case Double d    -> arena.allocateFrom(ValueLayout.JAVA_DOUBLE, d);
            case Float f     -> arena.allocateFrom(ValueLayout.JAVA_FLOAT, f);
            case Integer i   -> arena.allocateFrom(ValueLayout.JAVA_INT, i);
            case Long l      -> longAsInt()
                                    ? arena.allocateFrom(ValueLayout.JAVA_INT, l.intValue())
                                    : arena.allocateFrom(ValueLayout.JAVA_LONG, l);
            case Short s     -> arena.allocateFrom(ValueLayout.JAVA_SHORT, s);
            case Proxy p     -> p.handle();
            default          -> throw new IllegalArgumentException(
                    "Not a MemorySegment, String, primitive or Proxy");
        };
    }

    /**
     * Read an array of Strings with the requested length from native memory.
     *
     * @param  address address of the memory segment
     * @param  length  length of the array
     * @param  free    if the strings and the array must be freed
     * @return array of Strings
     */
    public static String[] getStringArrayFrom(MemorySegment address,
                                              int length,
                                              boolean free) {
        if (address == null || NULL.equals(address))
            return null;

        long size = ValueLayout.ADDRESS.byteSize();
        MemorySegment array = reinterpret(address, size * length);

        String[] result = new String[length];
        for (int i = 0; i < length; i++) {
            MemorySegment ptr = array.getAtIndex(ValueLayout.ADDRESS, i);
            result[i] = getStringFrom(ptr);
            if (free)
                GLib.free(array.getAtIndex(ValueLayout.ADDRESS, i));
        }

        if (free)
            GLib.free(array);
        return result;
    }

    /**
     * Read an array of Strings from a {@code NULL}-terminated array in native
     * memory.
     *
     * @param  address address of the memory segment
     * @param  free    if the strings and the array must be freed
     * @return array of Strings
     */
    public static String[] getStringArrayFrom(MemorySegment address,
                                              boolean free) {
        if (address == null || NULL.equals(address))
            return null;

        MemorySegment array = reinterpret(address, LONG_UNBOUNDED);

        ArrayList<String> result = new ArrayList<>();
        long offset = 0;
        while (true) {
            MemorySegment ptr = array.get(ValueLayout.ADDRESS, offset);
            if (NULL.equals(ptr))
                break;
            result.add(getStringFrom(ptr));
            offset += ValueLayout.ADDRESS.byteSize();
        }

        if (free)
            GLib.strfreev(array);

        return result.toArray(new String[0]);
    }

    /**
     * Read {@code NULL}-terminated arrays of Strings from a
     * {@code NULL}-terminated array in native memory.
     *
     * @param  address address of the memory segment
     * @param  free    if the strings and the array must be freed
     * @return two-dimensional array of Strings
     */
    public static String[][] getStrvArrayFrom(MemorySegment address,
                                              boolean free) {
        if (address == null || NULL.equals(address))
            return null;

        MemorySegment array = reinterpret(address, LONG_UNBOUNDED);

        ArrayList<String[]> result = new ArrayList<>();
        long offset = 0;
        while (true) {
            MemorySegment ptr = array.get(ValueLayout.ADDRESS, offset);
            if (NULL.equals(ptr))
                break;
            result.add(getStringArrayFrom(ptr, free));
            offset += ValueLayout.ADDRESS.byteSize();
        }

        if (free)
            GLib.free(address);

        return result.toArray(new String[0][0]);
    }

    /**
     * Read an array of pointers with the requested length from native memory.
     *
     * @param  address address of the memory segment
     * @param  length  length of the array
     * @param  free    if the array must be freed
     * @return array of pointers
     */
    public static MemorySegment[] getAddressArrayFrom(MemorySegment address,
                                                      int length,
                                                      boolean free) {

        if (address == null || NULL.equals(address))
            return null;

        long size = ValueLayout.ADDRESS.byteSize();
        MemorySegment array = reinterpret(address, size * length);

        MemorySegment[] result = new MemorySegment[length];
        for (int i = 0; i < length; i++)
            result[i] = array.getAtIndex(ValueLayout.ADDRESS, i);

        if (free)
            GLib.free(address);

        return result;
    }

    /**
     * Read an array of pointers from a {@code NULL}-terminated array in native
     * memory.
     *
     * @param  address address of the memory segment
     * @param  free    if the array must be freed
     * @return array of pointers
     */
    public static MemorySegment[] getAddressArrayFrom(MemorySegment address,
                                                      boolean free) {
        if (address == null || NULL.equals(address))
            return null;

        MemorySegment array = reinterpret(address, LONG_UNBOUNDED);

        ArrayList<MemorySegment> result = new ArrayList<>();
        long offset = 0;
        while (true) {
            MemorySegment ptr = array.get(ValueLayout.ADDRESS, offset);
            if (NULL.equals(ptr))
                break;
            result.add(ptr);
            offset += ValueLayout.ADDRESS.byteSize();
        }

        if (free)
            GLib.free(address);

        return result.toArray(new MemorySegment[0]);
    }

    /**
     * Read an array of booleans with the requested length from native memory.
     * The array is read from native memory as an array of integers with value
     * 1 or 0, and converted to booleans with 1 = true and 0 = false.
     *
     * @param  address address of the memory segment
     * @param  length  length of the array
     * @param  arena   the memory scope
     * @param  free    if the array must be freed
     * @return array of booleans
     */
    public static boolean[] getBooleanArrayFrom(MemorySegment address,
                                                long length,
                                                Arena arena,
                                                boolean free) {
        int[] intArray = getIntegerArrayFrom(address, length, arena, free);
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
     * @param  address address of the memory segment
     * @param  length  length of the array
     * @param  arena   the memory scope
     * @param  free    if the array must be freed
     * @return array of bytes
     */
    public static byte[] getByteArrayFrom(MemorySegment address,
                                          long length,
                                          Arena arena,
                                          boolean free) {
        if (address == null || NULL.equals(address))
            return null;

        byte[] array = address.reinterpret(length, arena, null)
                              .toArray(ValueLayout.JAVA_BYTE);

        if (free)
            GLib.free(address);

        return array;
    }

    /**
     * Read a {@code NULL}-terminated array of bytes from native memory.
     *
     * @param  address address of the memory segment
     * @param  arena   the memory scope
     * @param  free    if the array must be freed
     * @return array of bytes
     */
    public static byte[] getByteArrayFrom(MemorySegment address,
                                          Arena arena,
                                          boolean free) {
        if (address == null || NULL.equals(address))
            return null;

        // Find the null byte
        MemorySegment array = address.reinterpret(LONG_UNBOUNDED, arena, null);
        long idx = 0;
        while (array.get(ValueLayout.JAVA_BYTE, idx) != 0) {
            idx++;
        }

        return getByteArrayFrom(address, idx, arena, free);
    }

    /**
     * Read an array of chars with the requested length from native memory.
     *
     * @param  address address of the memory segment
     * @param  length  length of the array
     * @param  arena   the memory scope
     * @param  free    if the array must be freed
     * @return array of chars
     */
    public static char[] getCharacterArrayFrom(MemorySegment address,
                                               long length,
                                               Arena arena,
                                               boolean free) {
        if (address == null || NULL.equals(address))
            return null;

        long size = ValueLayout.JAVA_CHAR.byteSize();
        char[] array = address.reinterpret(length * size, arena, null)
                              .toArray(ValueLayout.JAVA_CHAR);

        if (free)
            GLib.free(address);

        return array;
    }

    /**
     * Read an array of doubles with the requested length from native memory.
     *
     * @param  address address of the memory segment
     * @param  length  length of the array
     * @param  arena   the memory scope
     * @param  free    if the array must be freed
     * @return array of doubles
     */
    public static double[] getDoubleArrayFrom(MemorySegment address,
                                              long length,
                                              Arena arena,
                                              boolean free) {
        if (address == null || NULL.equals(address))
            return null;

        long size = ValueLayout.JAVA_DOUBLE.byteSize();
        double[] array = address.reinterpret(length * size, arena, null)
                                .toArray(ValueLayout.JAVA_DOUBLE);

        if (free)
            GLib.free(address);

        return array;
    }

    /**
     * Read an array of floats with the requested length from native memory.
     *
     * @param  address address of the memory segment
     * @param  length  length of the array
     * @param  arena   the memory scope
     * @param  free    if the array must be freed
     * @return array of floats
     */
    public static float[] getFloatArrayFrom(MemorySegment address,
                                            long length,
                                            Arena arena,
                                            boolean free) {
        if (address == null || NULL.equals(address))
            return null;

        long size = ValueLayout.JAVA_FLOAT.byteSize();
        float[] array = address.reinterpret(length * size, arena, null)
                               .toArray(ValueLayout.JAVA_FLOAT);

        if (free)
            GLib.free(address);

        return array;
    }

    /**
     * Read a {@code NULL}-terminated array of float from native memory.
     *
     * @param  address address of the memory segment
     * @param  arena   the memory scope
     * @param  free    if the array must be freed
     * @return array of floats
     */
    public static float[] getFloatArrayFrom(MemorySegment address,
                                            Arena arena,
                                            boolean free) {
        if (address == null || NULL.equals(address))
            return null;

        // Find the null byte
        MemorySegment array = address.reinterpret(INT_UNBOUNDED, arena, null);
        long idx = 0;
        while (array.getAtIndex(ValueLayout.JAVA_FLOAT, idx) != 0) {
            idx++;
        }

        return getFloatArrayFrom(address, idx, arena, free);
    }

    /**
     * Read an array of integers with the requested length from native memory.
     *
     * @param  address address of the memory segment
     * @param  length  length of the array
     * @param  arena   the memory scope
     * @param  free    if the array must be freed
     * @return array of integers
     */
    public static int[] getIntegerArrayFrom(MemorySegment address,
                                            long length,
                                            Arena arena,
                                            boolean free) {
        if (address == null || NULL.equals(address))
            return null;

        long size = ValueLayout.JAVA_INT.byteSize();
        int[] array = address.reinterpret(length * size, arena, null)
                             .toArray(ValueLayout.JAVA_INT);

        if (free)
            GLib.free(address);

        return array;
    }

    /**
     * Read a {@code NULL}-terminated array of integers from native memory.
     *
     * @param  address address of the memory segment
     * @param  arena   the memory scope
     * @param  free    if the array must be freed
     * @return array of integers
     */
    public static int[] getIntegerArrayFrom(MemorySegment address,
                                            Arena arena,
                                            boolean free) {
        if (address == null || NULL.equals(address))
            return null;

        // Find the null byte
        MemorySegment array = address.reinterpret(INT_UNBOUNDED, arena, null);
        long idx = 0;
        while (array.getAtIndex(ValueLayout.JAVA_INT, idx) != 0) {
            idx++;
        }

        return getIntegerArrayFrom(address, idx, arena, free);
    }

    /**
     * Read an array of longs with the requested length from native memory.
     *
     * @param  address address of the memory segment
     * @param  length  length of the array
     * @param  arena   the memory scope
     * @param  free    if the array must be freed
     * @return array of longs
     */
    public static long[] getLongArrayFrom(MemorySegment address,
                                          long length,
                                          Arena arena,
                                          boolean free) {
        if (address == null || NULL.equals(address))
            return null;

        long size = ValueLayout.JAVA_LONG.byteSize();
        long[] array = address.reinterpret(length * size, arena, null)
                              .toArray(ValueLayout.JAVA_LONG);

        if (free)
            GLib.free(address);

        return array;
    }

    /**
     * Read a {@code NULL}-terminated array of longs from native memory.
     *
     * @param  address address of the memory segment
     * @param  arena   the memory scope
     * @param  free    if the array must be freed
     * @return array of longs
     */
    public static long[] getLongArrayFrom(MemorySegment address,
                                          Arena arena,
                                          boolean free) {
        if (address == null || NULL.equals(address))
            return null;

        // Find the null byte
        MemorySegment array = address.reinterpret(INT_UNBOUNDED, arena, null);
        long idx = 0;
        while (array.getAtIndex(ValueLayout.JAVA_LONG, idx) != 0) {
            idx++;
        }

        return getLongArrayFrom(address, idx, arena, free);
    }

    /**
     * Read an array of shorts with the requested length from native memory.
     *
     * @param  address address of the memory segment
     * @param  length  length of the array
     * @param  arena   the memory scope
     * @param  free    if the array must be freed
     * @return array of shorts
     */
    public static short[] getShortArrayFrom(MemorySegment address,
                                            long length,
                                            Arena arena,
                                            boolean free) {
        if (address == null || NULL.equals(address))
            return null;

        long size = ValueLayout.JAVA_SHORT.byteSize();
        short[] array = address.reinterpret(length * size, arena, null)
                               .toArray(ValueLayout.JAVA_SHORT);

        if (free)
            GLib.free(address);

        return array;
    }

    /**
     * Read a {@code NULL}-terminated array of shorts from native memory.
     *
     * @param  address address of the memory segment
     * @param  arena   the memory scope
     * @param  free    if the array must be freed
     * @return array of shorts
     */
    public static short[] getShortArrayFrom(MemorySegment address,
                                            Arena arena,
                                            boolean free) {
        if (address == null || NULL.equals(address))
            return null;

        // Find the null byte
        MemorySegment array = address.reinterpret(INT_UNBOUNDED, arena, null);
        long idx = 0;
        while (array.getAtIndex(ValueLayout.JAVA_SHORT, idx) != 0) {
            idx++;
        }

        return getShortArrayFrom(address, idx, arena, free);
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
        long offset = 0;
        while (!NULL.equals(array.get(ValueLayout.ADDRESS, offset))) {
            offset += ValueLayout.ADDRESS.byteSize();
        }

        return getProxyArrayFrom(address, (int) offset, cls, make);
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
            result[i] = make.apply(array.getAtIndex(ValueLayout.ADDRESS, i));
        }
        return result;
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
            result[i] = make.apply(array.getAtIndex(ValueLayout.JAVA_INT, i));
        }
        return result;
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * strings ({@code NULL}-terminated utf8 {@code char*}).
     *
     * @param  strings        array of Strings
     * @param  zeroTerminated whether to add a {@code NULL} to the array
     * @param  arena          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(String[] strings,
                                                    boolean zeroTerminated,
                                                    Arena arena) {
        if (strings == null)
            return NULL;

        int length = zeroTerminated ? strings.length + 1 : strings.length;
        var memorySegment = arena.allocate(ValueLayout.ADDRESS, length);

        for (int i = 0; i < strings.length; i++) {
            var s = strings[i] == null ? NULL : arena.allocateFrom(strings[i]);
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, s);
        }

        if (zeroTerminated)
            memorySegment.setAtIndex(ValueLayout.ADDRESS, strings.length, NULL);

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
     * @param  arena          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(String[][] strvs,
                                                    boolean zeroTerminated,
                                                    Arena arena) {
        if (strvs == null)
            return NULL;

        int length = zeroTerminated ? strvs.length + 1 : strvs.length;
        var memorySegment = arena.allocate(ValueLayout.ADDRESS, length);

        for (int i = 0; i < strvs.length; i++) {
            var s = strvs[i] == null ? NULL
                    : allocateNativeArray(strvs[i], true, arena);
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, s);
        }

        if (zeroTerminated)
            memorySegment.setAtIndex(ValueLayout.ADDRESS, strvs.length, NULL);

        return memorySegment;
    }

    /**
     * Convert a boolean[] array into an int[] array, and calls
     * {@link #allocateNativeArray(int[], boolean, Arena)}.
     * Each boolean value "true" is converted 1, boolean value "false" to 0.
     *
     * @param  array          array of booleans
     * @param  zeroTerminated when true, an (int) 0 is appended to the array
     * @param  arena          the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(boolean[] array,
                                                    boolean zeroTerminated,
                                                    Arena arena) {
        if (array == null)
            return NULL;

        int[] intArray = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            intArray[i] = array[i] ? 1 : 0;
        }

        return allocateNativeArray(intArray, zeroTerminated, arena);
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * bytes.
     *
     * @param  array          array of bytes
     * @param  zeroTerminated when true, a (byte) 0 is appended to the array
     * @param  arena          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(byte[] array,
                                                    boolean zeroTerminated,
                                                    Arena arena) {
        if (array == null)
            return NULL;

        byte[] copy = zeroTerminated ?
                Arrays.copyOf(array, array.length + 1)
                : array;

        return arena.allocateFrom(ValueLayout.JAVA_BYTE, copy);
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * chars.
     *
     * @param array          array of chars
     * @param zeroTerminated when true, a (char) 0 is appended to the array
     * @param arena          the segment allocator for memory allocation
     * @return whe memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(char[] array,
                                                    boolean zeroTerminated,
                                                    Arena arena) {
        if (array == null)
            return NULL;

        char[] copy = zeroTerminated ?
                Arrays.copyOf(array, array.length + 1)
                : array;

        return arena.allocateFrom(ValueLayout.JAVA_CHAR, copy);
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * doubles.
     *
     * @param  array          array of doubles
     * @param  zeroTerminated when true, a (double) 0 is appended to the array
     * @param  arena          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(double[] array,
                                                    boolean zeroTerminated,
                                                    Arena arena) {
        if (array == null)
            return NULL;

        double[] copy = zeroTerminated ?
                Arrays.copyOf(array, array.length + 1)
                : array;

        return arena.allocateFrom(ValueLayout.JAVA_DOUBLE, copy);
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * floats.
     *
     * @param  array          array of floats
     * @param  zeroTerminated when true, a (float) 0 is appended to the array
     * @param  arena          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(float[] array,
                                                    boolean zeroTerminated,
                                                    Arena arena) {
        if (array == null)
            return NULL;

        float[] copy = zeroTerminated ?
                Arrays.copyOf(array, array.length + 1)
                : array;

        return arena.allocateFrom(ValueLayout.JAVA_FLOAT, copy);
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * floats.
     *
     * @param  array          array of floats
     * @param  zeroTerminated when true, a (int) 0 is appended to the array
     * @param  arena          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(int[] array,
                                                    boolean zeroTerminated,
                                                    Arena arena) {
        if (array == null)
            return NULL;

        int[] copy = zeroTerminated ?
                Arrays.copyOf(array, array.length + 1)
                : array;
        return arena.allocateFrom(ValueLayout.JAVA_INT, copy);
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * longs.
     *
     * @param  array          array of longs
     * @param  zeroTerminated when true, a (long) 0 is appended to the array
     * @param  arena          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(long[] array,
                                                    boolean zeroTerminated,
                                                    Arena arena) {
        if (array == null)
            return NULL;

        long[] copy = zeroTerminated ?
                Arrays.copyOf(array, array.length + 1)
                : array;

        return arena.allocateFrom(ValueLayout.JAVA_LONG, copy);
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * shorts.
     *
     * @param  array          array of shorts
     * @param  zeroTerminated when true, a (short) 0 is appended to the array
     * @param  arena          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(short[] array,
                                                    boolean zeroTerminated,
                                                    Arena arena) {
        if (array == null)
            return NULL;

        short[] copy = zeroTerminated ?
                Arrays.copyOf(array, array.length + 1)
                : array;

        return arena.allocateFrom(ValueLayout.JAVA_SHORT, copy);
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * pointers (from Proxy instances).
     *
     * @param  array          array of Proxy instances
     * @param  zeroTerminated whether to add a {@code NULL} to the array
     * @param  arena          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(Proxy[] array,
                                                    boolean zeroTerminated,
                                                    Arena arena) {
        if (array == null)
            return NULL;

        MemorySegment[] addressArray = new MemorySegment[array.length];
        for (int i = 0; i < array.length; i++) {
            addressArray[i] = array[i] == null ? NULL : array[i].handle();
        }

        return allocateNativeArray(addressArray, zeroTerminated, arena);
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
     * @param  arena          the allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(Proxy[] array,
                                                    MemoryLayout layout,
                                                    boolean zeroTerminated,
                                                    Arena arena) {
        if (array == null)
            return NULL;

        long size = layout.byteSize();
        int length = zeroTerminated ? array.length + 1 : array.length;
        MemorySegment segment = arena.allocate(layout, length);

        for (int i = 0; i < array.length; i++) {
            if (array[i] != null && (!NULL.equals(array[i].handle()))) {
                // Copy array element to the native array
                MemorySegment element = array[i].handle()
                        .reinterpret(layout.byteSize(), arena, null);
                segment.asSlice(i * layout.byteSize()).copyFrom(element);
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
     * @param  arena          the segment allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(MemorySegment[] array,
                                                    boolean zeroTerminated,
                                                    Arena arena) {

        if (array == null)
            return NULL;

        int length = zeroTerminated ? array.length + 1 : array.length;
        var memorySegment = arena.allocate(ValueLayout.ADDRESS, length);

        for (int i = 0; i < array.length; i++) {
            MemorySegment s = array[i] == null ? NULL : array[i];
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, s);
        }

        if (zeroTerminated)
            memorySegment.setAtIndex(ValueLayout.ADDRESS, array.length, NULL);

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
    EnumSet<T> intToEnumSet(Class<T> cls,
                            Function<Integer, T> make,
                            int bitfield) {
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
                             LogLevelFlags.LEVEL_WARNING,
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
     * @param  <T> an enum implementing the Java-GI Enumeration interface
     * @param  set the set of enums
     * @return the resulting bitfield
     */
    public static <T extends Enum<T> & Enumeration>
    int enumSetToInt(Set<T> set) {
        int bitfield = 0;
        for (T element : set)
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
            MemorySegment _sizePointer = _arena.allocate(ValueLayout.JAVA_LONG);
            _sizePointer.set(ValueLayout.JAVA_LONG, 0L, 0L);
            Out<Long> size = new Out<>();
            MemorySegment _result;
            try {
                _result = (MemorySegment) DowncallHandles.g_bytes_get_data.invokeExact(address, _sizePointer);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
            size.set(_sizePointer.get(ValueLayout.JAVA_LONG, 0));
            return getByteArrayFrom(_result, size.get().intValue(), _arena, false);
        }
    }

    public static void freeGBytes(MemorySegment address) {
        try {
            DowncallHandles.g_bytes_unref.invokeExact(address);
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
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS,ValueLayout.JAVA_LONG),
                false);

        private static final MethodHandle g_bytes_get_data = Interop.downcallHandle(
                "g_bytes_get_data",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                false);

        private static final MethodHandle g_bytes_unref = Interop.downcallHandle(
                "g_bytes_unref",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                false);
    }
}
