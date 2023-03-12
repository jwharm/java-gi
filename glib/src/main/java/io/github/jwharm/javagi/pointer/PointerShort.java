package io.github.jwharm.javagi.pointer;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;

/**
 * A pointer to a short value.
 * Use {@code new PointerShort()} to create an instance, and
 * use {@link #get()} and {@link #set(Short)} to get and set the value.
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
     * @param address the memory address
     */
    public PointerShort(MemoryAddress address) {
        super(address);
    }

    /**
     * Create the pointer and point it to the given initial value.
     * @param initialValue the initial value
     */
    public PointerShort(short initialValue) {
        this();
        address.set(ValueLayout.JAVA_SHORT, 0, initialValue);
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(Short value) {
        address.set(ValueLayout.JAVA_SHORT, 0, value);
    }

    /**
     * Use this method to retrieve the value of the pointer.
     * @return the value of the pointer
     */
    public Short get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * <strong>Warning: There is no bounds checking.</strong>
     * @param index the array index
     * @return the value stored at the given index
     */
    public Short get(int index) {
        return address.get(
                ValueLayout.JAVA_SHORT,
                ValueLayout.JAVA_SHORT.byteSize() * index
        );
    }
}
