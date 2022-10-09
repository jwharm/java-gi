package io.github.jwharm.javagi;

import java.util.Iterator;

public class PointerIterator<T> implements Iterator<T> {

    public PointerIterator(Pointer<T> pointer) {
        this.pointer = pointer;
    }

    private final Pointer<T> pointer;
    private int index = 0;

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public T next() {
        return pointer.get(index++);
    }
}
