package io.github.jwharm.javagi.pointer;

import io.github.jwharm.javagi.interop.Interop;

import java.lang.foreign.MemoryAddress;

/**
 * A pointer to a char value.
 * Use {@code new PointerCharacter()} to create an instance, and
 * use {@link #get()} and {@link #set(Character)} to get and set the value.
 */
public class PointerCharacter extends Pointer<Character> {

    /**
     * Create the pointer. It does not point to a specific value.
     */
    public PointerCharacter() {
        super(Interop.valueLayout.C_CHAR);
    }

    /**
     * Create a pointer to an existing memory address.
     * @param address the memory address
     */
    public PointerCharacter(MemoryAddress address) {
        super(address);
    }

    /**
     * Create the pointer and point it to the given initial value.
     * @param initialValue the initial value
     */
    public PointerCharacter(char initialValue) {
        this();
        address.set(Interop.valueLayout.C_CHAR, 0, initialValue);
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(Character value) {
        address.set(Interop.valueLayout.C_CHAR, 0, value);
    }

    /**
     * Use this method to retrieve the value of the pointer.
     * @return the value of the pointer
     */
    public Character get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * <strong>Warning: There is no bounds checking.</strong>
     * @param index the array index
     * @return the value stored at the given index
     */
    public Character get(int index) {
        return address.get(
                Interop.valueLayout.C_CHAR,
                Interop.valueLayout.C_CHAR.byteSize() * index
        );
    }
}
