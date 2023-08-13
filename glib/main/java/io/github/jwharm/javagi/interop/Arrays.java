package io.github.jwharm.javagi.interop;

import io.github.jwharm.javagi.base.Proxy;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Array;
import java.util.List;
import java.util.function.Function;

public class Arrays {
    public static MemorySegment[] fromAddressPointer(MemorySegment address, int length) {
        MemorySegment segment = MemorySegment.ofAddress(address.address(), ValueLayout.ADDRESS.byteSize() * length, address.scope());
        MemorySegment[] array = new MemorySegment[length];
        for (int i = 0; i < length; i++) {
            array[i] = segment.getAtIndex(ValueLayout.ADDRESS, i);
        }
        return array;
    }

    public static String[] fromStringPointer(MemorySegment address, int length) {
        MemorySegment segment = MemorySegment.ofAddress(address.address(), ValueLayout.ADDRESS.byteSize() * length, address.scope());
        String[] array = new String[length];
        for (int i = 0; i < length; i++) {
            array[i] = segment.getAtIndex(ValueLayout.ADDRESS, i).getUtf8String(0);
        }
        return array;
    }

    public static <T> T[] fromIntPointer(MemorySegment address, int length, Class<T> clazz, Function<Integer, T> make) {
        MemorySegment segment = MemorySegment.ofAddress(address.address(), ValueLayout.JAVA_INT.byteSize() * length, address.scope());
        @SuppressWarnings("unchecked") T[] array = (T[]) Array.newInstance(clazz, length);
        for (int i = 0; i < length; i++) {
            array[i] = make.apply(segment.getAtIndex(ValueLayout.JAVA_INT, i));
        }
        return array;
    }

    public static <T extends Proxy> T[] fromPointer(MemorySegment address, Class<T> clazz, Function<MemorySegment, T> make) {
        long offset = 0;
        while (! MemorySegment.NULL.equals(address.get(ValueLayout.ADDRESS, offset))) {
            offset += ValueLayout.ADDRESS.byteSize();
        }
        return fromPointer(address, (int) offset, clazz, make);
    }

    public static <T extends Proxy> T[] fromPointer(MemorySegment address, int length, Class<T> clazz, Function<MemorySegment, T> make) {
        MemorySegment segment = MemorySegment.ofAddress(address.address(), ValueLayout.ADDRESS.byteSize() * length, address.scope());
        @SuppressWarnings("unchecked") T[] array = (T[]) Array.newInstance(clazz, length);
        for (int i = 0; i < length; i++) {
            array[i] = make.apply(segment.getAtIndex(ValueLayout.ADDRESS, i));
        }
        return array;
    }

    public static <T extends Proxy> T[] fromStructPointer(MemorySegment address, int length, Class<T> clazz, Function<MemorySegment, T> make, MemoryLayout layout) {
        var segment = MemorySegment.ofAddress(address.address(), layout.byteSize() * length, address.scope());
        @SuppressWarnings("unchecked") T[] array = (T[]) Array.newInstance(clazz, length);
        // MemorySegment.elements() only works for >1 elements
        if (length == 1) {
            array[0] = make.apply(segment);
        } else {
            List<MemorySegment> elements = segment.elements(layout).toList();
            for (int i = 0; i < length; i++) {
                array[i] = make.apply(elements.get(i));
            }
        }
        return array;
    }
}
