package io.github.jwharm.javagi;

import java.util.Iterator;

/**
 * This class provides an {@code Iterator} implementation for 
 * {@code Pointer} objects that does not check bounds, so you 
 * should avoid using this.
 * @param <T> The type of pointer
 */
public class PointerIterator<T> implements Iterator<T> {

    /**
     * Create a new PointerIterator for the provided pointer to an array
     * @param pointer the pointer to the array to iterate over
     */
    public PointerIterator(Pointer<T> pointer) {
        this.pointer = pointer;
    }

    private final Pointer<T> pointer;
    private int index = 0;

    /**
     * The iterator does not know how big the array is, so {@code hasNext()}
     * will always return {@code true}.
     */
    @Override
    public boolean hasNext() {
        return true;
    }

    /**
     * Returns the next element from the array in native memory.
     * <strong>Warning: There is no bounds checking.</strong>
     */
    @Override
    public T next() {
        return pointer.get(index++);
    }
}
