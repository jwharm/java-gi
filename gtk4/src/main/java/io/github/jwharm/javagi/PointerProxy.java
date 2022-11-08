package io.github.jwharm.javagi;

import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.InvocationTargetException;

/**
 * This type of Pointer object points to a GObject-derived object 
 * in native memory.
 * @param <T> The type of the object
 */
public class PointerProxy<T extends Proxy> extends Pointer<T> {

    private final Class<T> cls;

    /**
     * Create a pointer to an existing memory address.
     */
    public PointerProxy(MemoryAddress address, Class<T> cls) {
        super(address);
        this.cls = cls;
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(T value) {
        address.set(ValueLayout.ADDRESS, 0, value.handle());
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
     * <p>
     * <strong>Warning: There is no bounds checking.</strong>
     * <strong>Performance warning:</strong> This method uses reflection to instantiate the new object.
     * @param index The array index
     * @return The value stored at the given index
     */
    public T get(int index) {
        Refcounted ref = Refcounted.get(address.get(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS.byteSize() * index
        ));
        try {
            T instance = cls.getDeclaredConstructor(new Class[] {Refcounted.class}).newInstance(ref);
            return instance;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }
}
