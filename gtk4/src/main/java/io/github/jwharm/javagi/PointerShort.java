package io.github.jwharm.javagi;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;

/**
 * A pointer to a short value.
 * Use {@code new PointerShort()} to create an instance, and
 * use {@link #get()} afterwards to retreive the results.
 */
public class PointerShort extends Pointer<Short> {

    /**
     * Create the pointer. It does not point to a specific value.
     */
    public PointerShort() {
        super(ValueLayout.JAVA_SHORT);
    }

    /**
     * Create a pointer to an existing memory address.
     */
    public PointerShort(MemoryAddress address) {
        super(address);
    }

    /**
     * Create the pointer and point it to the given initial value.
     */
    public PointerShort(short initialValue) {
        this();
        address.set(ValueLayout.JAVA_SHORT, 0, initialValue);
    }

    /**
     * Use this mehod to set the value that the pointer points to.
     */
    public void set(Short value) {
        address.set(ValueLayout.JAVA_SHORT, 0, value);
    }

    /**
     * Use this method to retreive the value of the parameter after the
     * function call that set the value, has been executed.
     */
    public Short get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * @param index The array index
     * @return The value stored at the given index
     */
    public Short get(int index) {
        return address.get(
                ValueLayout.JAVA_SHORT,
                ValueLayout.JAVA_SHORT.byteSize() * index
        );
    }
}
