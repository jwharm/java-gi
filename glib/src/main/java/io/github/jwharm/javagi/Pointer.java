package io.github.jwharm.javagi;

import java.lang.foreign.*;

/**
 * A Pointer object represents a pointer to a value or array, where native 
 * code expects a pointer and Java-GI cannot marshall it to something nicer.
 * For example, use {@code PointerInteger} for an {@code *int} pointer.
 * @param <T> The type of the value that the pointer refers to
 */
public abstract class Pointer<T> implements Iterable<T> {

    protected final MemoryAddress address;

    /**
     * Allocate a new memory segment with the provided memory layout.
     * @param layout the memory layout
     */
    protected Pointer(ValueLayout layout) {
        MemorySegment segment = Interop.getAllocator().allocate(layout);
        this.address = segment.address();
    }

    /**
     * Instantiate a Pointer object that points to the provided address.
     * @param address The memory address
     */
    protected Pointer(MemoryAddress address) {
        this.address = address;
    }

    /**
     * Return the memory address of the pointer
     * @return the memory address
     */
    public Addressable handle() {
        return address;
    }

    /**
     * Get the value of the pointer
     * @return the value of the pointer
     */
    public abstract T get();
    
    /**
     * Get the value at the provided index of a pointer 
     * to an array in native memory
     * <strong>Warning: There is no bounds checking.</strong>
     * @param index the array index
     * @return the value at the provided index
     */
    public abstract T get(int index);
    
    /**
     * Set the value of the pointer
     * @param value the value to set
     */
    public abstract void set(T value);

    /**
     * Pointer objects implement {@code Iterable} so you 
     * can use pointers to an array in a for-each loop.
     * <strong>Warning: There is no bounds checking.</strong>
     */
    @Override
    public PointerIterator<T> iterator() {
        return new PointerIterator<>(this);
    }
}
