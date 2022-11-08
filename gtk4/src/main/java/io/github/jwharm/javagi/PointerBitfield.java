package io.github.jwharm.javagi;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.InvocationTargetException;

/**
 * A Pointer type that points to a bitfield
 * @param <T> The type of bitfield
 */
public class PointerBitfield<T extends Bitfield> extends Pointer<T> {

    private final Class<T> cls;

    /**
     * Create a pointer to an existing bitfield.
     * @param address the memory address
     * @param cls the type of bitfield
     */
    public PointerBitfield(MemoryAddress address, Class<T> cls) {
        super(address);
        this.cls = cls;
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(T value) {
        address.set(ValueLayout.JAVA_INT, 0, value.getValue());
    }

    /**
     * Use this method to retrieve the value of the pointer.
     * @return The value of the pointer
     */
    public T get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * <strong>Warning: There is no bounds checking.</strong>
     * <strong>Performance warning:</strong> This method uses reflection to instantiate the new object.
     * @param index The array index
     * @return The value stored at the given index
     */
    public T get(int index) {
        int value = address.get(
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_DOUBLE.byteSize() * index
        );
        try {
            T instance = cls.getDeclaredConstructor(new Class[] {Integer.class}).newInstance(value);
            return instance;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }
}
