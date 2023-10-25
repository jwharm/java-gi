/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
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

/**
 * The Interop class contains functionality for interoperability with native code.
 */
public class Interop {

    private record NamedFunction(String name, FunctionDescriptor fdesc, boolean variadic) {
    }

    private record FunctionPointer(MemorySegment address, FunctionDescriptor fdesc, boolean variadic) {
    }

    private static final Map<NamedFunction, MethodHandle> namedFunctions = new HashMap<>();
    private static final Map<FunctionPointer, MethodHandle> functionPointers = new HashMap<>();

    private final static SymbolLookup symbolLookup;
    private final static Linker linker = Linker.nativeLinker();

    static {
        SymbolLookup loaderLookup = SymbolLookup.loaderLookup();
        symbolLookup = name -> loaderLookup.find(name).or(() -> linker.defaultLookup().find(name));
    }

    /**
     * Convenience method that calls {@link #downcallHandle(String, FunctionDescriptor, boolean)} with variadic=false.
     *
     * @param name  Name of the native function
     * @param fdesc Function descriptor of the native function
     * @return the MethodHandle
     */
    public static MethodHandle downcallHandle(String name, FunctionDescriptor fdesc) {
        return downcallHandle(name, fdesc, false);
    }

    /**
     * Creates a method handle that is used to call the native function with the provided name and function descriptor.
     * The method handle is cached and reused in subsequent lookups.
     *
     * @param name     Name of the native function
     * @param fdesc    Function descriptor of the native function
     * @param variadic Whether the function has varargs
     * @return the MethodHandle
     */
    public static MethodHandle downcallHandle(String name, FunctionDescriptor fdesc, boolean variadic) {
        var func = new NamedFunction(name, fdesc, variadic);
        if (namedFunctions.containsKey(func)) {
            return namedFunctions.get(func);
        }
        // Copied from jextract-generated code
        var handle = symbolLookup.find(name).map(addr -> variadic ? VarargsInvoker.make(addr, fdesc)
                : linker.downcallHandle(addr, fdesc)).orElse(null);
        namedFunctions.put(func, handle);
        return handle;
    }

    /**
     * Creates a method handle that is used to call the native function at the provided memory address. The method
     * handle is cached and reused in subsequent lookups.
     *
     * @param symbol Memory address of the native function
     * @param fdesc  Function descriptor of the native function
     * @return the MethodHandle
     */
    public static MethodHandle downcallHandle(MemorySegment symbol, FunctionDescriptor fdesc) {
        var func = new FunctionPointer(symbol, fdesc, false);
        if (functionPointers.containsKey(func)) {
            return functionPointers.get(func);
        }
        var handle = linker.downcallHandle(symbol, fdesc);
        functionPointers.put(func, handle);
        return handle;
    }

    /**
     * Produce a method handle for a {@code upcall} method in the provided class.
     *
     * @param klazz      the callback class
     * @param descriptor the function descriptor for the native function
     * @return a method handle to use when creating an upcall stub
     */
    public static MethodHandle upcallHandle(MethodHandles.Lookup lookup,
                                            Class<?> klazz,
                                            FunctionDescriptor descriptor) {
        try {
            return lookup.findVirtual(klazz, "upcall", descriptor.toMethodType());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Produce a method handle for a method in the provided class.
     *
     * @param klazz      the callback class
     * @param name       the name of the callback method
     * @param descriptor the function descriptor for the native function
     * @return a method handle to use when creating an upcall stub
     */
    public static MethodHandle upcallHandle(MethodHandles.Lookup lookup,
                                            Class<?> klazz,
                                            String name,
                                            FunctionDescriptor descriptor) {
        try {
            return lookup.findVirtual(klazz, name, descriptor.toMethodType());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Register a Cleaner that will close the arena when the instance is garbage-collected,
     * coupling the lifetime of the arena to the lifetime of the instance.
     *
     * @param arena a memory arena that can be closed (normally {@link Arena#ofConfined()} or {@link Arena#ofAuto()}.
     * @param instance an object
     * @return the arena (for method chaining)
     */
    public static Arena attachArena(Arena arena, Object instance) {
        Cleaner cleaner = Cleaner.create();
        cleaner.register(instance, arena::close);
        return arena;
    }

    /**
     * Reinterpret {@code address} to {@code newSize} iff {@code newSize} is larger than the current size of
     * {@code address}.
     *
     * @param address a MemorySegment
     * @param newSize new size for the MemorySegment
     * @return the same MemorySegment reinterpreted at least {@code newSize}
     */
    public static MemorySegment reinterpret(MemorySegment address, long newSize) {
        if (address == null || address.byteSize() >= newSize) {
            return address;
        }
        return address.reinterpret(newSize);
    }

    /**
     * Get a gtype by executing the provided get-type function.
     *
     * @return The gtype from the provided get-type function
     */
    public static org.gnome.glib.Type getType(String getTypeFunction) {
        if (getTypeFunction == null) {
            return null;
        }
        FunctionDescriptor fdesc = FunctionDescriptor.of(ValueLayout.JAVA_LONG);
        try {
            MethodHandle handle = downcallHandle(getTypeFunction, fdesc, false);
            if (handle == null) {
                return null;
            }
            return new org.gnome.glib.Type((long) handle.invokeExact());
        } catch (Throwable err) {
            throw new AssertionError("Unexpected exception occured: ", err);
        }
    }

    /**
     * Allocate a native string using {@link SegmentAllocator#allocateUtf8String(String)}, but return
     * {@link MemorySegment#NULL} for a {@code null} argument.
     *
     * @param string    the string to allocate as a native string (utf8 char*) (can be {@code null})
     * @param allocator the segment allocator to use
     * @return the allocated MemorySegment with the native utf8 string, or {@link MemorySegment#NULL}
     */
    public static MemorySegment allocateNativeString(String string, SegmentAllocator allocator) {
        return string == null ? MemorySegment.NULL : allocator.allocateUtf8String(string);
    }

    /**
     * Returns a Java string from native memory using {@code MemorySegment.getUtf8String()}. If an error occurs or when
     * the native address is NULL, null is returned.
     *
     * @param address The memory address of the native String (\0-terminated char*).
     * @param free    if the address must be freed
     * @return A String or null
     */
    public static String getStringFrom(MemorySegment address, boolean free) {
        if (MemorySegment.NULL.equals(address)) {
            return null;
        }
        try {
            return address.reinterpret(Long.MAX_VALUE).getUtf8String(0);
        } finally {
            if (free) {
                GLib.free(address);
            }
        }
    }

    /**
     * Read an array of Strings with the given length from native memory
     *
     * @param address address of the memory segment
     * @param length  length of the array
     * @param free    if the strings and the array must be freed
     * @return Array of Strings
     */
    public static String[] getStringArrayFrom(MemorySegment address, int length, boolean free) {
        if (address == null || MemorySegment.NULL.equals(address)) {
            return null;
        }

        MemorySegment array = address;
        if (array.byteSize() == 0) {
            array = address.reinterpret(AddressLayout.ADDRESS.byteSize() * length);
        }

        String[] result = new String[length];
        for (int i = 0; i < length; i++) {
            result[i] = array.getUtf8String(i * ValueLayout.ADDRESS.byteSize());
            if (free) {
                GLib.free(array.getAtIndex(ValueLayout.ADDRESS, i));
            }
        }
        if (free) {
            GLib.free(array);
        }
        return result;
    }

    /**
     * Read an array of Strings from a null-terminated array in native memory
     *
     * @param address address of the memory segment
     * @param free    if the strings and the array must be freed
     * @return Array of Strings
     */
    public static String[] getStringArrayFrom(MemorySegment address, boolean free) {
        if (address == null || MemorySegment.NULL.equals(address)) {
            return null;
        }

        MemorySegment array = address;
        if (array.byteSize() == 0) {
            array = address.reinterpret(Long.MAX_VALUE);
        }

        ArrayList<String> result = new ArrayList<>();
        long offset = 0;
        while (true) {
            MemorySegment ptr = array.get(ValueLayout.ADDRESS, offset);
            if (MemorySegment.NULL.equals(ptr)) {
                break;
            }
            result.add(ptr.getUtf8String(0));
            offset += ValueLayout.ADDRESS.byteSize();
        }
        if (free) {
            GLib.free(address);
        }
        return result.toArray(new String[0]);
    }

    /**
     * Read an array of pointers with the given length from native memory
     *
     * @param address address of the memory segment
     * @param length  length of the array
     * @param free    if the addresses and the array must be freed
     * @return Array of pointers
     */
    public static MemorySegment[] getAddressArrayFrom(MemorySegment address, int length, boolean free) {
        if (address == null || MemorySegment.NULL.equals(address)) {
            return null;
        }

        MemorySegment array = address;
        if (array.byteSize() == 0) {
            array = address.reinterpret(AddressLayout.ADDRESS.byteSize() * length);
        }

        MemorySegment[] result = new MemorySegment[length];
        for (int i = 0; i < length; i++) {
            result[i] = array.getAtIndex(ValueLayout.ADDRESS, i);
            if (free) {
                GLib.free(array.getAtIndex(ValueLayout.ADDRESS, i));
            }
        }
        if (free) {
            GLib.free(address);
        }
        return result;
    }

    /**
     * Read an array of pointers from a null-terminated array in native memory
     *
     * @param address address of the memory segment
     * @param free    if the addresses and the array must be freed
     * @return Array of pointers
     */
    public static MemorySegment[] getAddressArrayFrom(MemorySegment address, boolean free) {
        if (address == null || MemorySegment.NULL.equals(address)) {
            return null;
        }

        MemorySegment array = address;
        if (array.byteSize() == 0) {
            array = address.reinterpret(Long.MAX_VALUE);
        }

        ArrayList<MemorySegment> result = new ArrayList<>();
        long offset = 0;
        while (true) {
            MemorySegment ptr = array.get(ValueLayout.ADDRESS, offset);
            if (MemorySegment.NULL.equals(ptr)) {
                break;
            }
            result.add(ptr);
            offset += ValueLayout.ADDRESS.byteSize();
        }
        if (free) {
            GLib.free(address);
        }
        return result.toArray(new MemorySegment[0]);
    }

    /**
     * Read an array of booleans with the given length from native memory The array is read from native memory as an
     * array of integers with value 1 or 0, and converted to booleans with 1 = true and 0 = false.
     *
     * @param address address of the memory segment
     * @param length  length of the array
     * @param arena   the memory scope
     * @param free    if the array must be freed
     * @return array of booleans
     */
    public static boolean[] getBooleanArrayFrom(MemorySegment address, long length, Arena arena, boolean free) {
        int[] intArray = getIntegerArrayFrom(address, length, arena, free);
        boolean[] array = new boolean[intArray.length];
        for (int c = 0; c < intArray.length; c++)
            array[c] = (intArray[c] != 0);
        return array;
    }

    /**
     * Read an array of bytes with the given length from native memory
     *
     * @param address address of the memory segment
     * @param length  length of the array
     * @param arena   the memory scope
     * @param free    if the array must be freed
     * @return array of bytes
     */
    public static byte[] getByteArrayFrom(MemorySegment address, long length, Arena arena, boolean free) {
        byte[] array = address.reinterpret(length, arena, null).toArray(ValueLayout.JAVA_BYTE);
        if (free) {
            GLib.free(address);
        }
        return array;
    }

    public static byte[] getByteArrayFrom(MemorySegment address, Arena arena, boolean free) {
        // Find the null byte
        MemorySegment array = address.reinterpret(Long.MAX_VALUE, arena, null);
        long idx = 0;
        while (array.get(ValueLayout.JAVA_BYTE, idx) != 0) {
            idx++;
        }
        return getByteArrayFrom(address, idx, arena, free);
    }

    /**
     * Read an array of chars with the given length from native memory
     *
     * @param address address of the memory segment
     * @param length  length of the array
     * @param arena   the memory scope
     * @param free    if the array must be freed
     * @return array of chars
     */
    public static char[] getCharacterArrayFrom(MemorySegment address, long length, Arena arena, boolean free) {
        char[] array = address.reinterpret(length, arena, null).toArray(ValueLayout.JAVA_CHAR);
        if (free) {
            GLib.free(address);
        }
        return array;
    }

    /**
     * Read an array of doubles with the given length from native memory
     *
     * @param address address of the memory segment
     * @param length  length of the array
     * @param arena   the memory scope
     * @param free    if the array must be freed
     * @return array of doubles
     */
    public static double[] getDoubleArrayFrom(MemorySegment address, long length, Arena arena, boolean free) {
        double[] array = address.reinterpret(length, arena, null).toArray(ValueLayout.JAVA_DOUBLE);
        if (free) {
            GLib.free(address);
        }
        return array;
    }

    /**
     * Read an array of floats with the given length from native memory
     *
     * @param address address of the memory segment
     * @param length  length of the array
     * @param arena   the memory scope
     * @param free    if the array must be freed
     * @return array of floats
     */
    public static float[] getFloatArrayFrom(MemorySegment address, long length, Arena arena, boolean free) {
        float[] array = address.reinterpret(length, arena, null).toArray(ValueLayout.JAVA_FLOAT);
        if (free) {
            GLib.free(address);
        }
        return array;
    }

    /**
     * Read an array of integers with the given length from native memory
     *
     * @param address address of the memory segment
     * @param length  length of the array
     * @param arena   the memory scope
     * @param free    if the array must be freed
     * @return array of integers
     */
    public static int[] getIntegerArrayFrom(MemorySegment address, long length, Arena arena, boolean free) {
        int[] array = address.reinterpret(length, arena, null).toArray(ValueLayout.JAVA_INT);
        if (free) {
            GLib.free(address);
        }
        return array;
    }

    /**
     * Read a null-terminated array of integers from native memory
     *
     * @param address address of the memory segment
     * @param arena   the memory scope
     * @param free    if the array must be freed
     * @return array of integers
     */
    public static int[] getIntegerArrayFrom(MemorySegment address, Arena arena, boolean free) {
        MemorySegment array = address.reinterpret(Integer.MAX_VALUE, arena, null);
        // Find the null value
        long idx = 0;
        while (array.get(ValueLayout.JAVA_INT, idx) != 0) {
            idx++;
        }
        return getIntegerArrayFrom(address, idx, arena, free);
    }

    /**
     * Read an array of longs with the given length from native memory
     *
     * @param address address of the memory segment
     * @param length  length of the array
     * @param arena   the memory scope
     * @param free    if the array must be freed
     * @return array of longs
     */
    public static long[] getLongArrayFrom(MemorySegment address, long length, Arena arena, boolean free) {
        long[] array = address.reinterpret(length, arena, null).toArray(ValueLayout.JAVA_LONG);
        if (free) {
            GLib.free(address);
        }
        return array;
    }

    /**
     * Read an array of shorts with the given length from native memory
     *
     * @param address address of the memory segment
     * @param length  length of the array
     * @param arena   the memory scope
     * @param free    if the array must be freed
     * @return array of shorts
     */
    public static short[] getShortArrayFrom(MemorySegment address, long length, Arena arena, boolean free) {
        short[] array = address.reinterpret(length, arena, null).toArray(ValueLayout.JAVA_SHORT);
        if (free) {
            GLib.free(address);
        }
        return array;
    }

    /**
     * Read a null-terminated array of memory addresses from native memory, create a Proxy instance for each address,
     * and return an array of Proxy instances
     *
     * @param address address of the memory segment
     * @param clazz   class of the Proxy type
     * @param make    constructor of the Proxy type
     * @param <T>     the type of the Proxy instances
     * @return array of Proxy instances
     */
    public static <T extends Proxy> T[] getProxyArrayFrom(MemorySegment address, Class<T> clazz, Function<MemorySegment, T> make) {
        if (address == null || MemorySegment.NULL.equals(address)) {
            return null;
        }

        MemorySegment array = address;
        if (array.byteSize() == 0) {
            array = address.reinterpret(Long.MAX_VALUE);
        }

        long offset = 0;
        while (!MemorySegment.NULL.equals(array.get(ValueLayout.ADDRESS, offset))) {
            offset += ValueLayout.ADDRESS.byteSize();
        }
        return getProxyArrayFrom(address, (int) offset, clazz, make);
    }

    /**
     * Read an array of memory addresses from native memory, create a Proxy instance for each address, and return an
     * array of Proxy instances
     *
     * @param address address of the memory segment
     * @param length  length of the array
     * @param clazz   class of the Proxy type
     * @param make    constructor of the Proxy type
     * @param <T>     the type of the Proxy instances
     * @return array of Proxy instances
     */
    public static <T extends Proxy> T[] getProxyArrayFrom(MemorySegment address, int length, Class<T> clazz, Function<MemorySegment, T> make) {
        if (address == null || MemorySegment.NULL.equals(address)) {
            return null;
        }

        MemorySegment array = address;
        if (array.byteSize() == 0) {
            array = address.reinterpret(AddressLayout.ADDRESS.byteSize() * length);
        }

        @SuppressWarnings("unchecked") T[] result = (T[]) Array.newInstance(clazz, length);
        for (int i = 0; i < length; i++) {
            result[i] = make.apply(array.getAtIndex(ValueLayout.ADDRESS, i));
        }
        return result;
    }

    /**
     * Read an array of structs from native memory, create a Proxy instance for each struct, and return an array of
     * Proxy instances
     *
     * @param address address of the memory segment
     * @param length  length of the array
     * @param clazz   class of the Proxy type
     * @param make    constructor of the Proxy type
     * @param <T>     the type of the Proxy instances
     * @return array of Proxy instances
     */
    public static <T extends Proxy> T[] getStructArrayFrom(MemorySegment address, int length, Class<T> clazz, Function<MemorySegment, T> make, MemoryLayout layout) {
        if (address == null || MemorySegment.NULL.equals(address)) {
            return null;
        }

        MemorySegment array = address;
        if (array.byteSize() == 0) {
            array = address.reinterpret(layout.byteSize() * length);
        }

        @SuppressWarnings("unchecked") T[] result = (T[]) Array.newInstance(clazz, length);
        List<MemorySegment> elements = array.elements(layout).toList();
        for (int i = 0; i < length; i++) {
            result[i] = make.apply(elements.get(i));
        }
        return result;
    }

    /**
     * Read an array of integers from native memory, create a Java instance for each integer value with the provided
     * constructor, and return an array of these instances. This is used to quickly create an array of Enumeration or
     * Bitfield objects from a native int array.
     *
     * @param address address of the memory segment
     * @param length  length of the array
     * @param clazz   class that will be returned in the array
     * @param make    constructor to create the instances
     * @param <T>     the type to construct
     * @return array of constructed instances
     */
    public static <T> T[] getArrayFromIntPointer(MemorySegment address, int length, Class<T> clazz, Function<Integer, T> make) {
        if (address == null || MemorySegment.NULL.equals(address)) {
            return null;
        }

        MemorySegment array = address;
        if (array.byteSize() == 0) {
            array = address.reinterpret(AddressLayout.ADDRESS.byteSize() * length);
        }

        @SuppressWarnings("unchecked") T[] result = (T[]) Array.newInstance(clazz, length);
        for (int i = 0; i < length; i++) {
            result[i] = make.apply(array.getAtIndex(ValueLayout.JAVA_INT, i));
        }
        return result;
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of strings (NUL-terminated utf8 char*).
     *
     * @param strings        Array of Strings
     * @param zeroTerminated Whether to add a NUL at the end the array
     * @param arena          the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(String[] strings, boolean zeroTerminated, Arena arena) {
        int length = zeroTerminated ? strings.length + 1 : strings.length;
        var memorySegment = arena.allocateArray(ValueLayout.ADDRESS, length);
        for (int i = 0; i < strings.length; i++) {
            var cString = strings[i] == null ? MemorySegment.NULL : arena.allocateUtf8String(strings[i]);
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, cString);
        }
        if (zeroTerminated) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, strings.length, MemorySegment.NULL);
        }
        return memorySegment;
    }

    /**
     * Converts the boolean[] array into an int[] array, and calls {@link #allocateNativeArray(int[], boolean, Arena)}.
     * Each boolean value "true" is converted 1, boolean value "false" to 0.
     *
     * @param array          Array of booleans
     * @param zeroTerminated When true, an (int) 0 is appended to the array
     * @param arena          the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(boolean[] array, boolean zeroTerminated, Arena arena) {
        int[] intArray = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            intArray[i] = array[i] ? 1 : 0;
        }
        return allocateNativeArray(intArray, zeroTerminated, arena);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of bytes.
     *
     * @param array          The array of bytes
     * @param zeroTerminated When true, a (byte) 0 is appended to the array
     * @param arena          the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(byte[] array, boolean zeroTerminated, Arena arena) {
        byte[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return arena.allocateArray(ValueLayout.JAVA_BYTE, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of chars.
     *
     * @param array          The array of chars
     * @param zeroTerminated When true, a (char) 0 is appended to the array
     * @param arena          the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(char[] array, boolean zeroTerminated, Arena arena) {
        char[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return arena.allocateArray(ValueLayout.JAVA_CHAR, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of doubles.
     *
     * @param array          The array of doubles
     * @param zeroTerminated When true, a (double) 0 is appended to the array
     * @param arena          the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(double[] array, boolean zeroTerminated, Arena arena) {
        double[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return arena.allocateArray(ValueLayout.JAVA_DOUBLE, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of floats.
     *
     * @param array          The array of floats
     * @param zeroTerminated When true, a (float) 0 is appended to the array
     * @param arena          the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(float[] array, boolean zeroTerminated, Arena arena) {
        float[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return arena.allocateArray(ValueLayout.JAVA_FLOAT, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of floats.
     *
     * @param array          The array of floats
     * @param zeroTerminated When true, a (int) 0 is appended to the array
     * @param arena          the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(int[] array, boolean zeroTerminated, Arena arena) {
        int[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return arena.allocateArray(ValueLayout.JAVA_INT, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of longs.
     *
     * @param array          The array of longs
     * @param zeroTerminated When true, a (long) 0 is appended to the array
     * @param arena          the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(long[] array, boolean zeroTerminated, Arena arena) {
        long[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return arena.allocateArray(ValueLayout.JAVA_LONG, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of shorts.
     *
     * @param array          The array of shorts
     * @param zeroTerminated When true, a (short) 0 is appended to the array
     * @param arena          the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(short[] array, boolean zeroTerminated, Arena arena) {
        short[] copy = zeroTerminated ? Arrays.copyOf(array, array.length + 1) : array;
        return arena.allocateArray(ValueLayout.JAVA_SHORT, copy);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of pointers (from Proxy instances).
     *
     * @param array          The array of Proxy instances
     * @param zeroTerminated Whether to add a NUL to the array
     * @param arena          the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(Proxy[] array, boolean zeroTerminated, Arena arena) {
        MemorySegment[] addressArray = new MemorySegment[array.length];
        for (int i = 0; i < array.length; i++) {
            addressArray[i] = array[i] == null ? MemorySegment.NULL : array[i].handle();
        }
        return allocateNativeArray(addressArray, zeroTerminated, arena);
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of structs (from Proxy instances). The actual
     * memory segments (not the pointers) are copied into the array.
     *
     * @param array          The array of Proxy instances
     * @param layout         The memory layout of the object type
     * @param zeroTerminated Whether to add a NUL to the array
     * @param arena          the allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(Proxy[] array, MemoryLayout layout, boolean zeroTerminated, Arena arena) {
        int length = zeroTerminated ? array.length + 1 : array.length;
        MemorySegment memorySegment = arena.allocateArray(layout, length);
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null && (!MemorySegment.NULL.equals(array[i].handle()))) {
                MemorySegment element = array[i].handle().reinterpret(layout.byteSize(), arena, null);
                memorySegment.asSlice(i * layout.byteSize()).copyFrom(element);
            } else {
                memorySegment.asSlice(i * layout.byteSize(), layout.byteSize()).fill((byte) 0);
            }
        }
        if (zeroTerminated) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, array.length, MemorySegment.NULL);
        }
        return memorySegment;
    }

    /**
     * Allocates and initializes an (optionally NULL-terminated) array of memory addresses.
     *
     * @param array          The array of addresses
     * @param zeroTerminated Whether to add a NUL to the array
     * @param arena          the segment allocator for memory allocation
     * @return The memory segment of the native array
     */
    public static MemorySegment allocateNativeArray(MemorySegment[] array, boolean zeroTerminated, Arena arena) {
        int length = zeroTerminated ? array.length + 1 : array.length;
        var memorySegment = arena.allocateArray(ValueLayout.ADDRESS, length);
        for (int i = 0; i < array.length; i++) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, array[i] == null ? MemorySegment.NULL : array[i]);
        }
        if (zeroTerminated) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, array.length, MemorySegment.NULL);
        }
        return memorySegment;
    }

    // Adapted from code that was generated by jextract
    private record VarargsInvoker(MemorySegment symbol, FunctionDescriptor function) {
        private static final MethodHandle INVOKE_MH;
        private static final SegmentAllocator THROWING_ALLOCATOR = (x, y) -> {
            throw new AssertionError("should not reach here");
        };

        static {
            try {
                INVOKE_MH = MethodHandles.lookup().findVirtual(VarargsInvoker.class, "invoke", MethodType.methodType(Object.class, SegmentAllocator.class, Object[].class));
            } catch (ReflectiveOperationException e) {
                throw new InteropException(e);
            }
        }

        static MethodHandle make(MemorySegment symbol, FunctionDescriptor function) {
            VarargsInvoker invoker = new VarargsInvoker(symbol, function);
            MethodHandle handle = INVOKE_MH.bindTo(invoker).asCollector(Object[].class, function.argumentLayouts().size() + 1);
            MethodType mtype = MethodType.methodType(function.returnLayout().isPresent() ? carrier(function.returnLayout().get(), true) : void.class);
            for (MemoryLayout layout : function.argumentLayouts()) {
                mtype = mtype.appendParameterTypes(carrier(layout, false));
            }
            mtype = mtype.appendParameterTypes(Object[].class);
            boolean needsAllocator = function.returnLayout().isPresent() && function.returnLayout().get() instanceof GroupLayout;
            if (needsAllocator) {
                mtype = mtype.insertParameterTypes(0, SegmentAllocator.class);
            } else {
                handle = MethodHandles.insertArguments(handle, 0, THROWING_ALLOCATOR);
            }
            return handle.asType(mtype);
        }

        static Class<?> carrier(MemoryLayout layout, boolean ret) {
            if (layout instanceof ValueLayout valLayout) {
                return (ret || valLayout.carrier() != MemorySegment.class) ? valLayout.carrier() : MemorySegment.class;
            } else if (layout instanceof GroupLayout) {
                return MemorySegment.class;
            } else {
                throw new AssertionError("Cannot get here!");
            }
        }

        // This method is used from a MethodHandle (INVOKE_MH).
        @SuppressWarnings("unused")
        private Object invoke(SegmentAllocator allocator, Object[] args) throws Throwable {
            // one trailing Object[]
            int nNamedArgs = function.argumentLayouts().size();
            assert (args.length == nNamedArgs + 1);
            // The last argument is the array of vararg collector
            Object[] unnamedArgs = (Object[]) args[args.length - 1];

            int argsCount = nNamedArgs + unnamedArgs.length;
            MemoryLayout[] argLayouts = new MemoryLayout[nNamedArgs + unnamedArgs.length];

            int pos = 0;
            for (pos = 0; pos < nNamedArgs; pos++) {
                argLayouts[pos] = function.argumentLayouts().get(pos);
            }

            // Unwrap the java-gi types to their memory address or primitive value
            Object[] unwrappedArgs = new Object[unnamedArgs.length];
            for (int i = 0; i < unnamedArgs.length; i++) {
                unwrappedArgs[i] = unwrapJavagiTypes(unnamedArgs[i]);
            }

            assert pos == nNamedArgs;
            for (Object o : unwrappedArgs) {
                argLayouts[pos] = variadicLayout(normalize(o.getClass()));
                pos++;
            }
            assert pos == argsCount;

            FunctionDescriptor f = (function.returnLayout().isEmpty()) ? FunctionDescriptor.ofVoid(argLayouts) : FunctionDescriptor.of(function.returnLayout().get(), argLayouts);
            MethodHandle mh = linker.downcallHandle(symbol, f);
            boolean needsAllocator = function.returnLayout().isPresent() && function.returnLayout().get() instanceof GroupLayout;
            if (needsAllocator) {
                mh = mh.bindTo(allocator);
            }
            // flatten argument list so that it can be passed to an asSpreader MH
            Object[] allArgs = new Object[nNamedArgs + unwrappedArgs.length];
            System.arraycopy(args, 0, allArgs, 0, nNamedArgs);
            System.arraycopy(unwrappedArgs, 0, allArgs, nNamedArgs, unwrappedArgs.length);

            return mh.asSpreader(Object[].class, argsCount).invoke(allArgs);
        }

        private static Class<?> unboxIfNeeded(Class<?> clazz) {
            if (clazz == Boolean.class) {
                return boolean.class;
            } else if (clazz == Void.class) {
                return void.class;
            } else if (clazz == Byte.class) {
                return byte.class;
            } else if (clazz == Character.class) {
                return char.class;
            } else if (clazz == Short.class) {
                return short.class;
            } else if (clazz == Integer.class) {
                return int.class;
            } else if (clazz == Long.class) {
                return long.class;
            } else if (clazz == Float.class) {
                return float.class;
            } else if (clazz == Double.class) {
                return double.class;
            } else {
                return clazz;
            }
        }

        private Class<?> promote(Class<?> c) {
            if (c == byte.class || c == char.class || c == short.class || c == int.class) {
                return long.class;
            } else if (c == float.class) {
                return double.class;
            } else {
                return c;
            }
        }

        private Class<?> normalize(Class<?> c) {
            c = unboxIfNeeded(c);
            if (c.isPrimitive()) {
                return promote(c);
            }
            if (MemorySegment.class.isAssignableFrom(c)) {
                return MemorySegment.class;
            }
            throw new IllegalArgumentException("Invalid type for ABI: " + c.getTypeName());
        }

        private MemoryLayout variadicLayout(Class<?> c) {
            if (c == long.class) {
                return ValueLayout.JAVA_LONG;
            } else if (c == double.class) {
                return ValueLayout.JAVA_DOUBLE;
            } else if (MemorySegment.class.isAssignableFrom(c)) {
                return ValueLayout.ADDRESS;
            } else {
                throw new IllegalArgumentException("Unhandled variadic argument class: " + c);
            }
        }

        // Unwrap the java-gi types to their memory address or primitive value.
        // Arrays are allocated to native memory as-is (no additional NUL is appended: the caller must do this)
        private Object unwrapJavagiTypes(Object o) {
            if (o == null) {
                return MemorySegment.NULL;
            }
            if (o instanceof MemorySegment[] addresses) {
                return Interop.allocateNativeArray(addresses, false, Arena.ofAuto()).address();
            }
            if (o instanceof Boolean bool) {
                return bool ? 1 : 0;
            }
            if (o instanceof boolean[] values) {
                return Interop.allocateNativeArray(values, false, Arena.ofAuto()).address();
            }
            if (o instanceof byte[] values) {
                return Interop.allocateNativeArray(values, false, Arena.ofAuto()).address();
            }
            if (o instanceof char[] values) {
                return Interop.allocateNativeArray(values, false, Arena.ofAuto()).address();
            }
            if (o instanceof double[] values) {
                return Interop.allocateNativeArray(values, false, Arena.ofAuto()).address();
            }
            if (o instanceof float[] values) {
                return Interop.allocateNativeArray(values, false, Arena.ofAuto()).address();
            }
            if (o instanceof int[] values) {
                return Interop.allocateNativeArray(values, false, Arena.ofAuto()).address();
            }
            if (o instanceof long[] values) {
                return Interop.allocateNativeArray(values, false, Arena.ofAuto()).address();
            }
            if (o instanceof short[] values) {
                return Interop.allocateNativeArray(values, false, Arena.ofAuto()).address();
            }
            if (o instanceof Proxy proxy) {
                return proxy.handle();
            }
            if (o instanceof Proxy[] proxys) {
                return Interop.allocateNativeArray(proxys, false, Arena.ofAuto()).address();
            }
            if (o instanceof Alias<?> alias) {
                return alias.getValue();
            }
            if (o instanceof Bitfield bitfield) {
                return bitfield.getValue();
            }
            if (o instanceof Bitfield[] bitfields) {
                return Bitfield.getValues(bitfields);
            }
            if (o instanceof Enumeration enumeration) {
                return enumeration.getValue();
            }
            if (o instanceof Enumeration[] enumerations) {
                return Enumeration.getValues(enumerations);
            }
            if (o instanceof String string) {
                return Interop.allocateNativeString(string, Arena.ofAuto()).address();
            }
            if (o instanceof String[] strings) {
                return Interop.allocateNativeArray(strings, false, Arena.ofAuto()).address();
            }
            return o;
        }
    }
}
