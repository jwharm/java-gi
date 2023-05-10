package io.github.jwharm.javagi.pointer;

import io.github.jwharm.javagi.interop.Interop;

import java.lang.foreign.*;
import java.lang.foreign.MemorySegment;

/**
 * This class implements a pointer to a String in native memory.
 */
public class PointerString extends Pointer<String> {

    private MemorySegment allocatedString;

    /**
     * Create the pointer. It does not have an initial value set.
     */
    public PointerString() {
        super(ValueLayout.ADDRESS);
    }

    /**
     * Create the pointer and set the provided initial value.
     * @param initialValue the initial value
     */
    public PointerString(String initialValue) {
        this();
        set(initialValue);
    }

    /**
     * Create a pointer to an existing memory address.
     * @param address the memory address
     */
    public PointerString(MemorySegment address) {
        super(address);
    }

    /**
     * Allocate a native string and set the pointer to its address.
     * The native memory is released when the {@code PointerString} instance
     * becomes unreachable and is garbage collected.
     * When {@code set(String)} is called multiple times for the same
     * {@code PointerString} instance, the previously allocated memory
     * segment will also be released during garbage collection.
     * @param value the new string that is pointed to
     */
    public void set(String value) {
        this.allocatedString = Interop.allocateNativeString(value, SegmentAllocator.nativeAllocator(SegmentScope.global()));
        segment.set(ValueLayout.ADDRESS, 0, this.allocatedString);
    }

    /**
     * Allocate a native string and set the pointer to its address.
     * @param value the new string that is pointed to
     * @param allocator the memory allocator for the native string
     */
    public void set(String value, SegmentAllocator allocator) {
        segment.set(ValueLayout.ADDRESS, 0, Interop.allocateNativeString(value, allocator));
    }

    /**
     * Use this method to retrieve the value of the pointer.
     * @return the value of the pointer
     */
    public String get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * <strong>Warning: There is no bounds checking.</strong>
     * @param index the array index
     * @return the value stored at the given index
     */
    public String get(int index) {
        return segment.get(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS.byteSize() * index
        ).getUtf8String(0);
    }
}
