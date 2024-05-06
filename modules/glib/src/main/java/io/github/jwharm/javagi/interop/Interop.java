/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 Jan-Willem Harmannij
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

import io.github.jwharm.javagi.base.Enumeration;
import org.gnome.glib.GLib;

import io.github.jwharm.javagi.base.*;
import org.gnome.glib.Type;

/**
 * The Interop class contains functionality for interoperability with native
 * code.
 */
public class Interop {

    private final static int UNBOUNDED = Integer.MAX_VALUE;
    private final static Linker LINKER = Linker.nativeLinker();
    public static SymbolLookup symbolLookup = SymbolLookup.loaderLookup()
            .or(Linker.nativeLinker().defaultLookup());

    /**
     * Load the specified library using
     * {@link SymbolLookup#libraryLookup(String, Arena)}.
     *
     * @param name the name of the library
     */
    public static void loadLibrary(String name) {
        try {
            symbolLookup = SymbolLookup.libraryLookup(name, Arena.global())
                                       .or(Interop.symbolLookup);
        } catch (IllegalArgumentException iae) {
            LibLoad.loadLibrary(name);
        }
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
     * @param  symbol memory address of the native function
     * @param  fdesc  function descriptor of the native function
     * @return the newly created MethodHandle
     */
    public static MethodHandle downcallHandle(MemorySegment symbol,
                                              FunctionDescriptor fdesc) {
        return LINKER.downcallHandle(symbol, fdesc);
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
        return string == null
                ? MemorySegment.NULL
                : alloc.allocateFrom(string);
    }

    /**
     * Copy a Java string from native memory using
     * {@code MemorySegment.getUtf8String()}. If an error occurs or when the
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
     * {@code MemorySegment.getUtf8String()}. If an error occurs or when the
     * native address is NULL, null is returned.
     *
     * @param  address the memory address of the native String
     *                 (a {@code NULL}-terminated {@code char*})
     * @param  free    if the address must be freed
     * @return a String or null
     */
    public static String getStringFrom(MemorySegment address, boolean free) {

        if (MemorySegment.NULL.equals(address))
            return null;

        try {
            return address.reinterpret(Long.MAX_VALUE).getString(0);
        } finally {
            if (free)
                GLib.free(address);
        }
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

        if (address == null || MemorySegment.NULL.equals(address))
            return null;

        MemorySegment array = address;
        long size = ValueLayout.ADDRESS.byteSize();
        if (array.byteSize() == 0)
            array = address.reinterpret(size * length);

        String[] result = new String[length];
        for (int i = 0; i < length; i++) {
            result[i] = array.getString(i * size);
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
        if (address == null || MemorySegment.NULL.equals(address))
            return null;

        MemorySegment array = address;
        if (array.byteSize() == 0) {
            array = address.reinterpret(Long.MAX_VALUE);
        }

        ArrayList<String> result = new ArrayList<>();
        long offset = 0;
        while (true) {
            MemorySegment ptr = array.get(ValueLayout.ADDRESS, offset);
            if (MemorySegment.NULL.equals(ptr))
                break;
            result.add(ptr.getString(0));
            offset += ValueLayout.ADDRESS.byteSize();
        }

        if (free)
            GLib.free(address);

        return result.toArray(new String[0]);
    }

    /**
     * Read an array of pointers with the requested length from native memory.
     *
     * @param  address address of the memory segment
     * @param  length  length of the array
     * @param  free    if the addresses and the array must be freed
     * @return array of pointers
     */
    public static MemorySegment[] getAddressArrayFrom(MemorySegment address,
                                                      int length,
                                                      boolean free) {

        if (address == null || MemorySegment.NULL.equals(address))
            return null;

        long size = ValueLayout.ADDRESS.byteSize();
        MemorySegment array = address;
        if (array.byteSize() == 0)
            array = address.reinterpret(size * length);

        MemorySegment[] result = new MemorySegment[length];
        for (int i = 0; i < length; i++) {
            result[i] = array.getAtIndex(ValueLayout.ADDRESS, i);
            if (free)
                GLib.free(array.getAtIndex(ValueLayout.ADDRESS, i));
        }

        if (free)
            GLib.free(address);

        return result;
    }

    /**
     * Read an array of pointers from a {@code NULL}-terminated array in native
     * memory.
     *
     * @param  address address of the memory segment
     * @param  free    if the addresses and the array must be freed
     * @return array of pointers
     */
    public static MemorySegment[] getAddressArrayFrom(MemorySegment address,
                                                      boolean free) {

        if (address == null || MemorySegment.NULL.equals(address))
            return null;

        MemorySegment array = address;
        if (array.byteSize() == 0)
            array = address.reinterpret(Long.MAX_VALUE);

        ArrayList<MemorySegment> result = new ArrayList<>();
        long offset = 0;
        while (true) {
            MemorySegment ptr = array.get(ValueLayout.ADDRESS, offset);
            if (MemorySegment.NULL.equals(ptr))
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
        // Find the null byte
        MemorySegment array = address.reinterpret(Long.MAX_VALUE, arena, null);
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

        char[] array = address.reinterpret(length, arena, null)
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

        double[] array = address.reinterpret(length, arena, null)
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

        float[] array = address.reinterpret(length, arena, null)
                .toArray(ValueLayout.JAVA_FLOAT);

        if (free)
            GLib.free(address);

        return array;
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

        int[] array = address.reinterpret(length, arena, null)
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

        // Find the null byte
        MemorySegment array = address.reinterpret(UNBOUNDED, arena, null);
        long idx = 0;
        while (array.get(ValueLayout.JAVA_INT, idx) != 0) {
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

        long[] array = address.reinterpret(length, arena, null)
                .toArray(ValueLayout.JAVA_LONG);

        if (free)
            GLib.free(address);

        return array;
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

        short[] array = address.reinterpret(length, arena, null)
                .toArray(ValueLayout.JAVA_SHORT);

        if (free)
            GLib.free(address);

        return array;
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
    public static <T extends Proxy> T[]
    getProxyArrayFrom(MemorySegment address,
                      Class<T> cls,
                      Function<MemorySegment, T> make) {

        if (address == null || MemorySegment.NULL.equals(address))
            return null;

        MemorySegment array = address;
        if (array.byteSize() == 0)
            array = address.reinterpret(Long.MAX_VALUE);

        long offset = 0;
        while (!MemorySegment.NULL.equals(
                        array.get(ValueLayout.ADDRESS, offset))) {
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
    public static <T extends Proxy> T[]
    getProxyArrayFrom(MemorySegment address,
                      int length,
                      Class<T> cls,
                      Function<MemorySegment, T> make) {

        if (address == null || MemorySegment.NULL.equals(address))
            return null;

        long size = AddressLayout.ADDRESS.byteSize();
        MemorySegment array = address;
        if (array.byteSize() == 0)
            array = address.reinterpret(size * length);

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
    public static <T extends Proxy> T[]
    getStructArrayFrom(MemorySegment address,
                       int length,
                       Class<T> cls,
                       Function<MemorySegment, T> make,
                       MemoryLayout layout) {

        if (address == null || MemorySegment.NULL.equals(address))
            return null;

        MemorySegment array = address;
        if (array.byteSize() == 0)
            array = address.reinterpret(layout.byteSize() * length);

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

        if (address == null || MemorySegment.NULL.equals(address))
            return null;

        long size = AddressLayout.ADDRESS.byteSize();
        MemorySegment array = address;
        if (array.byteSize() == 0)
            array = address.reinterpret(size * length);

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

        int length = zeroTerminated ? strings.length + 1 : strings.length;
        var memorySegment = arena.allocate(ValueLayout.ADDRESS, length);

        for (int i = 0; i < strings.length; i++) {
            var cString = strings[i] == null ? MemorySegment.NULL
                    : arena.allocateFrom(strings[i]);
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, cString);
        }

        if (zeroTerminated)
            memorySegment.setAtIndex(
                    ValueLayout.ADDRESS, strings.length, MemorySegment.NULL);

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

        MemorySegment[] addressArray = new MemorySegment[array.length];
        for (int i = 0; i < array.length; i++) {
            addressArray[i] = array[i] == null
                    ? MemorySegment.NULL
                    : array[i].handle();
        }

        return allocateNativeArray(addressArray, zeroTerminated, arena);
    }

    /**
     * Allocate and initialize an (optionally {@code NULL}-terminated) array of
     * structs (from Proxy instances). The actual memory segments (not the
     * pointers) are copied into the array.
     *
     * @param  array          array of Proxy instances
     * @param  layout         the memory layout of the object type
     * @param  zeroTerminated whether to add a {@code NULL} to the array
     * @param  arena          the allocator for memory allocation
     * @return the memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(Proxy[] array,
                                                    MemoryLayout layout,
                                                    boolean zeroTerminated,
                                                    Arena arena) {

        int length = zeroTerminated ? array.length + 1 : array.length;
        MemorySegment memorySegment = arena.allocate(layout, length);

        for (int i = 0; i < array.length; i++) {
            if (array[i] != null
                    && (!MemorySegment.NULL.equals(array[i].handle()))) {
                // Copy array element to the native array
                MemorySegment element = array[i].handle()
                        .reinterpret(layout.byteSize(), arena, null);
                memorySegment.asSlice(i * layout.byteSize())
                        .copyFrom(element);
            } else {
                // Fill the array with zeros
                long size = layout.byteSize();
                memorySegment.asSlice(i * size, size).fill((byte) 0);
            }
        }

        if (zeroTerminated)
            memorySegment.setAtIndex(
                    ValueLayout.ADDRESS, array.length, MemorySegment.NULL);

        return memorySegment;
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

        int length = zeroTerminated ? array.length + 1 : array.length;
        var memorySegment = arena.allocate(ValueLayout.ADDRESS, length);

        for (int i = 0; i < array.length; i++) {
            MemorySegment s = array[i] == null ? MemorySegment.NULL : array[i];
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, s);
        }

        if (zeroTerminated)
            memorySegment.setAtIndex(
                    ValueLayout.ADDRESS, array.length, MemorySegment.NULL);

        return memorySegment;
    }

    /**
     * Create an EnumSet of class `cls` from the provided bitfield
     *
     * @param  <T>      an enum implementing the Java-GI Enumeration interface
     * @param  cls      the class of the enum
     * @param  bitfield the integer containing the bitfield
     * @return an EnumSet containing the enum values as set in the bitfield
     */
    public static <T extends Enum<T> & Enumeration>
    EnumSet<T> intToEnumSet(Class<T> cls, int bitfield) {
        try {
            int flags = bitfield;
            EnumSet<T> enumSet = EnumSet.noneOf(cls);
            MethodType mt = MethodType.methodType(cls, int.class);
            MethodHandle enumOf = MethodHandles.lookup()
                                               .findStatic(cls, "of", mt);
            while (flags != 0) {
                int flag = Integer.numberOfTrailingZeros(flags);
                    @SuppressWarnings("unchecked")
                    T enumValue = (T) enumOf.invokeExact(flag);
                    enumSet.add(enumValue);
                flags -= Integer.lowestOneBit(flags);
            }
            return enumSet;
        } catch (Throwable err) {
            throw new AssertionError("Unexpected exception occurred: ", err);
        }
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
        for (T value : set)
            bitfield |= value.getValue();
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
}
