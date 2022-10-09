package io.github.jwharm.javagi;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;

/**
 * A pointer to an int value.
 * Use {@code new PointerInteger()} to create an instance, and
 * use {@link #get()} afterwards to retreive the results.
 */
public class PointerInteger extends Pointer<Integer> {

    /**
     * Create the pointer. It does not point to a specific value.
     */
    public PointerInteger() {
        super(ValueLayout.JAVA_INT);
    }

    /**
     * Create a pointer to an existing memory address.
     */
    public PointerInteger(MemoryAddress address) {
        super(address);
    }

    /**
     * Create the pointer and point it to the given initial value.
     */
    public PointerInteger(int initialValue) {
        this();
        address.set(ValueLayout.JAVA_INT, 0, initialValue);
    }

    /**
     * Use this mehod to set the value that the pointer points to.
     */
    public void set(Integer value) {
        address.set(ValueLayout.JAVA_INT, 0, value);
    }

    /**
     * Use this method to retreive the value of the parameter after the
     * function call that set the value, has been executed.
     */
    public Integer get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * @param index The array index
     * @return The value stored at the given index
     */
    public Integer get(int index) {
        return address.get(
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT.byteSize() * index
        );
    }
}
