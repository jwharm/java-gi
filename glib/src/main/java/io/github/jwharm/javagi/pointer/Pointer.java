package io.github.jwharm.javagi.pointer;

import org.gnome.glib.GLib;

import java.lang.foreign.*;
import java.lang.reflect.Array;

/**
 * A Pointer object represents a pointer to a value or array, where native 
 * code expects a pointer and Java-GI cannot marshall it to something nicer.
 * For example, use {@code PointerInteger} for an {@code *int} pointer.
 * @param <T> The type of the value that the pointer refers to
 */
public abstract class Pointer<T> implements Iterable<T> {

    private final MemorySegment segment;

    /**
     * The memory address of the pointer
     */
    protected final MemoryAddress address;

    /**
     * Allocate a new memory segment with the provided memory layout.
     * @param layout the memory layout
     */
    protected Pointer(ValueLayout layout) {
        this.segment = MemorySession.openImplicit().allocate(layout);
        this.address = segment.address();
    }

    /**
     * Instantiate a Pointer object that points to the provided address.
     * @param address The memory address
     */
    protected Pointer(MemoryAddress address) {
        this.segment = null;
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

    /**
     * Release the pointer. This will run {@link GLib#free(MemoryAddress)}
     * on the native memory address.
     */
    public void free() {
        GLib.free(address);
    }

    /**
     * Read an array of values from the pointer
     * @param length length of the array
     * @param clazz type of the array elements
     * @param free if the pointer must be freed
     * @return the array
     */
    public T[] toArray(int length, Class<T> clazz, boolean free) {
        @SuppressWarnings("unchecked")
        T[] array = (T[]) Array.newInstance(clazz, length);
        for (int i = 0; i < length; i++) {
            array[i] = get(i);
        }
        if (free) {
            GLib.free(address);
        }
        return array;
    }

}
