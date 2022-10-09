package io.github.jwharm.javagi;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;

public class PointerString extends Pointer<String> {

    /**
     * Create the pointer. It does not have an initial value set.
     */
    public PointerString() {
        super(ValueLayout.ADDRESS);
    }

    public PointerString(String initialValue) {
        this();
        set(initialValue);
    }

    /**
     * Create a pointer to an existing memory address.
     */
    public PointerString(MemoryAddress address) {
        super(address);
    }

    /**
     * Use this mehod to set the value that the pointer points to.
     */
    public void set(String value) {
        address.set(ValueLayout.ADDRESS, 0, Interop.allocateNativeString(value).handle());
    }

    /**
     * Use this method to retreive the value of the parameter after the
     * function call that set the value, has been executed.
     */
    public String get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * @param index The array index
     * @return The value stored at the given index
     */
    public String get(int index) {
        return address.get(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS.byteSize() * index
        ).getUtf8String(0);
    }
}
