package io.github.jwharm.javagi;

import java.lang.foreign.MemoryAddress;
import java.util.function.Function;

/**
 * A pointer type that points to an enumeration
 * @param <T> the type of enumeration
 */
public class PointerEnumeration<T extends Enumeration> extends Pointer<T> {

    private final Function<Integer, T> make;

    /**
     * Create a pointer to an existing enumeration.
     * @param address the memory address
     * @param make a function to create an enumeration instance of an int
     */
    public PointerEnumeration(MemoryAddress address, Function<Integer, T> make) {
        super(address);
        this.make = make;
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(T value) {
        address.set(Interop.valueLayout.C_INT, 0, value.getValue());
    }

    /**
     * Use this method to retrieve the value of the pointer.
     * @return the value of the pointer
     */
    public T get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * <p>
     * <strong>Warning: There is no bounds checking.</strong>
     * @param index the array index
     * @return the value stored at the given index
     */
    public T get(int index) {
        int value = address.get(
                Interop.valueLayout.C_INT,
                Interop.valueLayout.C_INT.byteSize() * index
        );
        return make.apply(value);
    }
}
