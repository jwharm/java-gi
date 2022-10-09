package io.github.jwharm.javagi;

import java.lang.foreign.*;

/**
 * A Pointer object contains a pointer to a primitive value that is used when in C, a pointer to
 * a primitive value is expected, for example as an out-parameter.
 * For example, use {@code PointerInteger} for an {@code *int}
 * out-parameter.
 */
public abstract class Pointer<T> implements Iterable<T> {

    protected final MemoryAddress address;
    private MemorySegmentReference reference;

    /**
     * Allocate a new memory segment with the provided memory layout.
     */
    protected Pointer(ValueLayout layout) {
        MemorySegment segment = Interop.getAllocator().allocate(layout);
        reference = new MemorySegmentReference(segment);
        this.address = segment.address();
    }

    /**
     * Instantiate a Pointer object that points to the provided address.
     */
    public Pointer(MemoryAddress address) {
        this.address = address;
    }

    public Addressable handle() {
        return address;
    }

    public abstract T get();
    public abstract T get(int index);
    public abstract void set(T value);

    @Override
    public PointerIterator<T> iterator() {
        return new PointerIterator<T>(this);
    }
}
