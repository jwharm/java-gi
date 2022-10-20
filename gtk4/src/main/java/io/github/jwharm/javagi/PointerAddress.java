package io.github.jwharm.javagi;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;

public class PointerAddress extends Pointer<MemoryAddress> {

    /**
     * Create the pointer. It does not point to a specific address.
     */
    public PointerAddress() {
        super(ValueLayout.ADDRESS);
    }
    
    /**
     * Create a pointer to an existing memory address.
     */
    public PointerAddress(MemoryAddress address) {
        super(address);
    }

    /**
     * Use this method to set the value that the pointer points to.
     */
    public void set(MemoryAddress value) {
        address.set(ValueLayout.ADDRESS, 0, value);
    }

    /**
     * Use this method to retrieve the value of the parameter after the
     * function call that set the value, has been executed.
     */
    public MemoryAddress get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * <p>
     * <strong>Performance warning:</strong> This method uses reflection to instantiate the new object.
     * @param index The array index
     * @return The value stored at the given index
     */
    public MemoryAddress get(int index) {
        return address.get(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS.byteSize() * index
        );
    }
}
