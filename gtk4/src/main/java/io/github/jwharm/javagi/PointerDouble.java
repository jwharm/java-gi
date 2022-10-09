package io.github.jwharm.javagi;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;

/**
 * A pointer to a double value.
 * Use {@code new PointerDouble()} to create an instance, and
 * use {@link #get()} afterwards to retreive the results.
 */
public class PointerDouble extends Pointer<Double> {

    /**
     * Create the pointer. It does not point to a specific value.
     */
    public PointerDouble() {
        super(ValueLayout.JAVA_DOUBLE);
    }

    /**
     * Create a pointer to an existing memory address.
     */
    public PointerDouble(MemoryAddress address) {
        super(address);
    }

    /**
     * Create the pointer and point it to the given initial value.
     */
    public PointerDouble(double initialValue) {
        this();
        address.set(ValueLayout.JAVA_DOUBLE, 0, initialValue);
    }

    /**
     * Use this mehod to set the value that the pointer points to.
     */
    public void set(Double value) {
        address.set(ValueLayout.JAVA_DOUBLE, 0, value);
    }

    /**
     * Use this method to retreive the value of the parameter after the
     * function call that set the value, has been executed.
     */
    public Double get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * @param index The array index
     * @return The value stored at the given index
     */
    public Double get(int index) {
        return address.get(
                ValueLayout.JAVA_DOUBLE,
                ValueLayout.JAVA_DOUBLE.byteSize() * index
        );
    }
}
