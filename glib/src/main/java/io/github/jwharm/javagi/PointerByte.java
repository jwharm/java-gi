package io.github.jwharm.javagi;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;

/**
 * A pointer to a byte value.
 * Use {@code new PointerByte()} to create an instance, and
 * use {@link #get()} and {@link #set(Byte)} to get and set the value.
 */
public class PointerByte extends Pointer<Byte> {

    /**
     * Create the pointer. It does not point to a specific value.
     */
    public PointerByte() {
        super(Interop.valueLayout.C_BYTE);
    }

    /**
     * Create a pointer to an existing memory address.
     * @param address the memory address
     */
    public PointerByte(MemoryAddress address) {
        super(address);
    }

    /**
     * Create the pointer and point it to the given initial value.
     * @param initialValue the initial value
     */
    public PointerByte(byte initialValue) {
        this();
        address.set(Interop.valueLayout.C_BYTE, 0, initialValue);
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(Byte value) {
        address.set(Interop.valueLayout.C_BYTE, 0, value);
    }

    /**
     * Use this method to retrieve the value of the pointer.
     * @return The value of the pointer
     */
    public Byte get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * <strong>Warning: There is no bounds checking.</strong>
     * @param index the array index
     * @return the value stored at the given index
     */
    public Byte get(int index) {
        return address.get(
                Interop.valueLayout.C_BYTE,
                Interop.valueLayout.C_BYTE.byteSize() * index
        );
    }
}
