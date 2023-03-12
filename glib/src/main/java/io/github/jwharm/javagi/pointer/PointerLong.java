package io.github.jwharm.javagi.pointer;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;

/**
 * A pointer to a long value.
 * Use {@code new PointerLong()} to create an instance, and
 * use {@link #get()} and {@link #set(Long)} to get and set the value.
 */
public class PointerLong extends Pointer<Long> {

    /**
     * Create the pointer. It does not point to a specific value.
     */
    public PointerLong() {
        super(ValueLayout.JAVA_LONG);
    }

    /**
     * Create a pointer to an existing memory address.
     * @param address the memory address
     */
    public PointerLong(MemoryAddress address) {
        super(address);
    }

    /**
     * Create the pointer and point it to the given initial value.
     * @param initialValue the initial value
     */
    public PointerLong(long initialValue) {
        this();
        address.set(ValueLayout.JAVA_LONG, 0, initialValue);
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(Long value) {
        address.set(ValueLayout.JAVA_LONG, 0, value);
    }

    /**
     * Use this method to retrieve the value of the pointer.
     * @return the value of the pointer
     */
    public Long get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * <strong>Warning: There is no bounds checking.</strong>
     * @param index the array index
     * @return the value stored at the given index
     */
    public Long get(int index) {
        return address.getAtIndex(ValueLayout.JAVA_LONG, index);
    }
}
