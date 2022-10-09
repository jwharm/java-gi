package io.github.jwharm.javagi;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;

/**
 * A pointer to a byte value.
 * Use {@code new PointerByte()} to create an instance, and
 * use {@link #get()} afterwards to retrieve the results.
 */
public class PointerByte extends Pointer<Byte> {

    /**
     * Create the pointer. It does not point to a specific value.
     */
    public PointerByte() {
        super(ValueLayout.JAVA_BYTE);
    }

    /**
     * Create a pointer to an existing memory address.
     */
    public PointerByte(MemoryAddress address) {
        super(address);
    }

    /**
     * Create the pointer and point it to the given initial value.
     */
    public PointerByte(byte initialValue) {
        this();
        address.set(ValueLayout.JAVA_BYTE, 0, initialValue);
    }

    /**
     * Use this method to set the value that the pointer points to.
     */
    public void set(Byte value) {
        address.set(ValueLayout.JAVA_BYTE, 0, value);
    }

    /**
     * Use this method to retrieve the value of the parameter after the
     * function call that set the value, has been executed.
     */
    public Byte get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * @param index The array index
     * @return The value stored at the given index
     */
    public Byte get(int index) {
        return address.get(
                ValueLayout.JAVA_BYTE,
                ValueLayout.JAVA_BYTE.byteSize() * index
        );
    }
}
