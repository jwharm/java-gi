package io.github.jwharm.javagi;

import java.lang.foreign.MemoryAddress;

/**
 * A pointer to a float value.
 * Use {@code new PointerFloat()} to create an instance, and
 * use {@link #get()} and {@link #set(Float)} to get and set the value.
 */
public class PointerFloat extends Pointer<Float> {

    /**
     * Create the pointer. It does not point to a specific value.
     */
    public PointerFloat() {
        super(Interop.valueLayout.C_FLOAT);
    }

    /**
     * Create a pointer to an existing memory address.
     * @param address the memory address
     */
    public PointerFloat(MemoryAddress address) {
        super(address);
    }

    /**
     * Create the pointer and point it to the given initial value.
     * @param initialValue the initial value
     */
    public PointerFloat(float initialValue) {
        this();
        address.set(Interop.valueLayout.C_FLOAT, 0, initialValue);
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(Float value) {
        address.set(Interop.valueLayout.C_FLOAT, 0, value);
    }

    /**
     * Use this method to retrieve the value of the pointer.
     * @return the value of the pointer
     */
    public Float get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * <strong>Warning: There is no bounds checking.</strong>
     * @param index the array index
     * @return the value stored at the given index
     */
    public Float get(int index) {
        return address.get(
                Interop.valueLayout.C_FLOAT,
                Interop.valueLayout.C_FLOAT.byteSize() * index
        );
    }
}
