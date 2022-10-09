package io.github.jwharm.javagi;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;

/**
 * A pointer to a char value.
 * Use {@code new PointerCharacter()} to create an instance, and
 * use {@link #get()} afterwards to retreive the results.
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
     */
    public PointerCharacter(MemoryAddress address) {
        super(address);
    }

    /**
     * Create the pointer and point it to the given initial value.
     */
    public PointerCharacter(char initialValue) {
        this();
        address.set(ValueLayout.JAVA_CHAR, 0, initialValue);
    }

    /**
     * Use this mehod to set the value that the pointer points to.
     */
    public void set(Character value) {
        address.set(ValueLayout.JAVA_CHAR, 0, value);
    }

    /**
     * Use this method to retreive the value of the parameter after the
     * function call that set the value, has been executed.
     */
    public Character get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * @param index The array index
     * @return The value stored at the given index
     */
    public Character get(int index) {
        return address.get(
                ValueLayout.JAVA_CHAR,
                ValueLayout.JAVA_CHAR.byteSize() * index
        );
    }
}
