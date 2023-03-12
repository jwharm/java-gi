package io.github.jwharm.javagi.pointer;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;

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
        super(ValueLayout.JAVA_CHAR);
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
        address.set(ValueLayout.JAVA_CHAR, 0, initialValue);
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(Character value) {
        address.set(ValueLayout.JAVA_CHAR, 0, value);
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
        return address.getAtIndex(ValueLayout.JAVA_CHAR, index);
    }
}
