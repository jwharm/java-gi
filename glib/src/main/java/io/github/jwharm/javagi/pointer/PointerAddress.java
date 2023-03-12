package io.github.jwharm.javagi.pointer;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;

/**
 * A pointer that points to a raw memory address
 */
public class PointerAddress extends Pointer<MemoryAddress> {

    /**
     * Create the pointer. It does not point to a specific address.
     */
    public PointerAddress() {
        super(ValueLayout.ADDRESS);
    }
    
    /**
     * Create a pointer to an existing memory address.
     * @param address the memory address
     */
    public PointerAddress(MemoryAddress address) {
        super(address);
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(MemoryAddress value) {
        address.set(ValueLayout.ADDRESS, 0, value);
    }

    /**
     * Use this method to retrieve the value of the pointer.
     * @return the value of the pointer
     */
    public MemoryAddress get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * <strong>Warning: There is no bounds checking.</strong>
     * <p>
     * @param index the array index
     * @return the value stored at the given index
     */
    public MemoryAddress get(int index) {
        return address.getAtIndex(ValueLayout.ADDRESS, index);
    }
}
