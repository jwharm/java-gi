package io.github.jwharm.javagi;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;

/**
 * This class implements a pointer to a String in native memory.
 */
public class PointerString extends Pointer<String> {

    /**
     * Create the pointer. It does not have an initial value set.
     */
    public PointerString() {
        super(ValueLayout.ADDRESS);
    }

    /**
     * Create the pointer and set the provided initial value.
     * @param initialValue The initial value
     */
    public PointerString(String initialValue) {
        this();
        set(initialValue);
    }

    /**
     * Create a pointer to an existing memory address.
     * @param address the memory address
     */
    public PointerString(MemoryAddress address) {
        super(address);
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(String value) {
        address.set(ValueLayout.ADDRESS, 0, Interop.allocateNativeString(value));
    }

    /**
     * Use this method to retrieve the value of the pointer.
     * @return The value of the pointer
     */
    public String get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * <strong>Warning: There is no bounds checking.</strong>
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
