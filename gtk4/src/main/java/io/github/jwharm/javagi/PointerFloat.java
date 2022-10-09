package io.github.jwharm.javagi;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;

/**
 * A pointer to a float value.
 * Use {@code new PointerFloat()} to create an instance, and
 * use {@link #get()} afterwards to retreive the results.
 */
public class PointerFloat extends Pointer<Float> {

    /**
     * Create the pointer. It does not point to a specific value.
     */
    public PointerFloat() {
        super(ValueLayout.JAVA_FLOAT);
    }

    /**
     * Create a pointer to an existing memory address.
     */
    public PointerFloat(MemoryAddress address) {
        super(address);
    }

    /**
     * Create the pointer and point it to the given initial value.
     */
    public PointerFloat(float initialValue) {
        this();
        address.set(ValueLayout.JAVA_FLOAT, 0, initialValue);
    }

    /**
     * Use this mehod to set the value that the pointer points to.
     */
    public void set(Float value) {
        address.set(ValueLayout.JAVA_FLOAT, 0, value);
    }

    /**
     * Use this method to retreive the value of the parameter after the
     * function call that set the value, has been executed.
     */
    public Float get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * @param index The array index
     * @return The value stored at the given index
     */
    public Float get(int index) {
        return address.get(
                ValueLayout.JAVA_FLOAT,
                ValueLayout.JAVA_FLOAT.byteSize() * index
        );
    }
}
