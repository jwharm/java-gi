package io.github.jwharm.javagi.pointer;

import io.github.jwharm.javagi.base.Marshal;
import io.github.jwharm.javagi.base.Proxy;
import io.github.jwharm.javagi.interop.Interop;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryAddress;

/**
 * This type of Pointer object points to a GObject-derived object 
 * in native memory.
 * @param <T> The type of the object
 */
public class PointerProxy<T extends Proxy> extends Pointer<T> {

    private final Marshal<Addressable, ? extends T> make;

    /**
     * Create a pointer to an existing memory address.
     * @param address the memory address
     * @param make a function to create an instance
     */
    public PointerProxy(MemoryAddress address, Marshal<Addressable, ? extends T> make) {
        super(address);
        this.make = make;
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(T value) {
        address.set(Interop.valueLayout.ADDRESS, 0, value.handle());
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
        // Get the memory address of the native object.
        Addressable ref = address.get(
                Interop.valueLayout.ADDRESS,
                Interop.valueLayout.ADDRESS.byteSize() * index
        );
        // Call the constructor of the proxy object and return the created instance.
        // TODO: What about ownership of the object?
        return make.marshal(ref, null);
    }
}
