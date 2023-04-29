package io.github.jwharm.javagi.pointer;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * A pointer to an int value.
 * Use {@code new PointerInteger()} to create an instance, and
 * use {@link #get()} and {@link #set(Integer)} to get and set the value.
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
     * @param address the memory address
     */
    public PointerInteger(MemorySegment address) {
        super(address);
    }

    /**
     * Create the pointer and point it to the given initial value.
     * @param initialValue the initial value
     */
    public PointerInteger(int initialValue) {
        this();
        segment.set(ValueLayout.JAVA_INT, 0, initialValue);
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(Integer value) {
        segment.set(ValueLayout.JAVA_INT, 0, value);
    }

    /**
     * Use this method to retrieve the value of the pointer.
     * @return the value of the pointer
     */
    public Integer get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * <strong>Warning: There is no bounds checking.</strong>
     * @param index the array index
     * @return the value stored at the given index
     */
    public Integer get(int index) {
        return segment.getAtIndex(ValueLayout.JAVA_INT, index);
    }
}
