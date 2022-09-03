package io.github.jwharm.javagi.interop;

import jdk.incubator.foreign.*;

public class Interop {

    private static boolean initialized = false;
    private static ResourceScope scope;
    private static SegmentAllocator allocator;

    private static void initialize() {
        scope = ResourceScope.newConfinedScope();
        allocator = SegmentAllocator.nativeAllocator(scope);
        initialized = true;
    }

    public static ResourceScope getScope() {
        if (!initialized) {
            initialize();
        }
        return scope;
    }

    public static SegmentAllocator getAllocator() {
        if (!initialized) {
            initialize();
        }
        return allocator;
    }

    /**
     * Allocates and initializes a NULL-terminated array of strings (NUL-terminated utf8 char*).
     */
    public static Addressable allocateNativeArray(String[] strings) {
        if (!initialized) {
            initialize();
        }
        if (strings == null || strings.length == 0) {
            return MemoryAddress.NULL;
        }
        var memorySegment = allocator.allocateArray(ValueLayout.ADDRESS, strings.length + 1);
        for (int i = 0; i < strings.length; i++) {
            var cString = allocator.allocateUtf8String(strings[i]);
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, cString);
        }
        memorySegment.setAtIndex(ValueLayout.ADDRESS, strings.length, MemoryAddress.NULL);
        return memorySegment;
    }

    public static Addressable allocateNativeArray(boolean[] array) {
        if (!initialized) {
            initialize();
        }
        if (array == null || array.length == 0) {
            return MemoryAddress.NULL;
        }
        int[] intArray = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            intArray[i] = array[i] ? 1 : 0;
        }
        return allocator.allocateArray(ValueLayout.JAVA_INT, intArray);
    }

    public static Addressable allocateNativeArray(org.gtk.gobject.Type[] array) {
        if (!initialized) {
            initialize();
        }
        if (array == null || array.length == 0) {
            return MemoryAddress.NULL;
        }
        long[] longArray = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            longArray[i] = array[i].getValue();
        }
        return allocator.allocateArray(ValueLayout.JAVA_LONG, longArray);
    }

    public static Addressable allocateNativeArray(org.gtk.gobject.Value[] array) {
        if (!initialized) {
            initialize();
        }
        if (array == null || array.length == 0) {
            return MemoryAddress.NULL;
        }
        MemorySegment mem = io.github.jwharm.javagi.interop.jextract.GValue.allocateArray(array.length, allocator);
        long size = io.github.jwharm.javagi.interop.jextract.GValue.sizeof();
        for (int i = 0; i < array.length; i++) {
            MemorySegment source = MemorySegment.ofAddress(array[i].HANDLE(), size, scope);
            MemorySegment target = mem.asSlice(i * size, size);
            target.copyFrom(source);
        }
        return mem.address();
    }

    /**
     * Allocates and initializes a NULL-terminated array of pointers (from NativeAddress instances).
     */
    public static Addressable allocateNativeArray(NativeAddress[] array) {
        if (!initialized) {
            initialize();
        }
        if (array == null || array.length == 0) {
            return MemoryAddress.NULL;
        }
        var memorySegment = allocator.allocateArray(ValueLayout.ADDRESS, array.length + 1);
        for (int i = 0; i < array.length; i++) {
            memorySegment.setAtIndex(ValueLayout.ADDRESS, i, array[i].HANDLE());
        }
        memorySegment.setAtIndex(ValueLayout.ADDRESS, array.length, MemoryAddress.NULL);
        return memorySegment;
    }
}
