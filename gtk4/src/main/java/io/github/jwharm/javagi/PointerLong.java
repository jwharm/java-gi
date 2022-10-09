package io.github.jwharm.javagi;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;

/**
 * A pointer to a long value.
 * Use {@code new PointerLong()} to create an instance, and
 * use {@link #get()} afterwards to retrieve the results.
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
     */
    public PointerLong(MemoryAddress address) {
        super(address);
    }

    /**
     * Create the pointer and point it to the given initial value.
     */
    public PointerLong(long initialValue) {
        this();
        address.set(ValueLayout.JAVA_LONG, 0, initialValue);
    }

    /**
     * Use this method to set the value that the pointer points to.
     */
    public void set(Long value) {
        address.set(ValueLayout.JAVA_LONG, 0, value);
    }

    /**
     * Use this method to retrieve the value of the parameter after the
     * function call that set the value, has been executed.
     */
    public Long get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * @param index The array index
     * @return The value stored at the given index
     */
    public Long get(int index) {
        return address.get(
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_LONG.byteSize() * index
        );
    }
}
