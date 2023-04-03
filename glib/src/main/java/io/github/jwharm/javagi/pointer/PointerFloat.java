package io.github.jwharm.javagi.pointer;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

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
        super(ValueLayout.JAVA_FLOAT);
    }

    /**
     * Create a pointer to an existing memory address.
     * @param address the memory address
     */
    public PointerFloat(MemorySegment address) {
        super(address);
    }

    /**
     * Create the pointer and point it to the given initial value.
     * @param initialValue the initial value
     */
    public PointerFloat(float initialValue) {
        this();
        segment.set(ValueLayout.JAVA_FLOAT, 0, initialValue);
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(Float value) {
        segment.set(ValueLayout.JAVA_FLOAT, 0, value);
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
        return segment.getAtIndex(ValueLayout.JAVA_FLOAT, index);
    }
}
