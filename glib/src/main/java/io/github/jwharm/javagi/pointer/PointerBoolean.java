package io.github.jwharm.javagi.pointer;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * A pointer to a boolean value.
 * Use {@code new PointerBoolean()} to create an instance, and
 * use {@link #get()} and {@link #set(Boolean)} to get and set the value.
 */
public class PointerBoolean extends Pointer<Boolean> {

    /**
     * Create the pointer. It does not point to a specific value.
     */
    public PointerBoolean() {
        super(ValueLayout.JAVA_INT);
    }

    /**
     * Create a pointer to an existing memory address.
     * @param address the memory address
     */
    public PointerBoolean(MemorySegment address) {
        super(address);
    }

    /**
     * Create the pointer and point it to the given initial value.
     * @param initialValue the initial value
     */
    public PointerBoolean(boolean initialValue) {
        this();
        segment.set(ValueLayout.JAVA_INT, 0, initialValue ? 1 : 0);
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(Boolean value) {
        segment.set(ValueLayout.JAVA_INT, 0, value ? 1 : 0);
    }

    /**
     * Use this method to retrieve the value that the pointer points to.
     * @return the value of the pointer
     */
    public Boolean get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * <strong>Warning: There is no bounds checking.</strong>
     * @param index the array index
     * @return the value stored at the given index
     */
    public Boolean get(int index) {
        return segment.getAtIndex(ValueLayout.JAVA_INT, index) != 0;
    }
}
