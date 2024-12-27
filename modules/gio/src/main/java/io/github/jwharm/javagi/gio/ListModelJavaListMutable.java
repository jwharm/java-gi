package io.github.jwharm.javagi.gio;

import org.gnome.gobject.GObject;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ListModelJavaListMutable<E extends GObject> extends ListModelJavaList<E> {

    void removeAt(int index);
    void append(E e);

    /**
     * {@inheritDoc}
     *
     * @apiNote This implementation delegates to {@link #append(GObject)}.
     */
    @Override
    default boolean add(E e) {
        append(e);
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This implementation removes all subsequent elements, appends the new element, and then appends the removed elements, skipping the element at the specified index.
     */
    @Override
    default E set(int index, E element) {
        int size = size();
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException();
        List<E> subList = subList(index, size);
        List<E> buffer = List.copyOf(subList);
        subList.clear();
        append(element);
        boolean first = true;
        E result = null;
        for (E e : buffer) {
            if (first) {
                result = e;
                first = false;
            } else {
                append(e);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This implementation removes all subsequent elements, appends the new element, and then appends all the removed elements.
     */
    @Override
    default void add(int index, E element) {
        int size = size();
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException();
        List<E> subList = subList(index, size);
        List<E> buffer = List.copyOf(subList);
        subList.clear();
        append(element);
        for (E e : buffer) {
            append(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This implementation delegates to {@link #removeAt(int)} and {@link #getItem(int)}.
     */
    @Override
    default E remove(int index) {
        E e = getItem(index);
        removeAt(index);
        return e;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default @NotNull List<E> subList(int fromIndex, int toIndex) {
        return new SubList<>(this, fromIndex, toIndex);
    }

    @ApiStatus.Internal
    class SubList<E extends GObject, List extends ListModelJavaListMutable<E>> extends ListModelJavaList.SubList<E, List> implements ListModelJavaListMutable<E> {
        public SubList(List list, int fromIndex, int toIndex) {
            super(list, fromIndex, toIndex);
        }

        @Override
        public void removeAt(int index) {
            if (index < 0 || index >= size())
                throw new IndexOutOfBoundsException();
            list.removeAt(index + fromIndex);
            toIndex--;
        }

        @Override
        public void append(E e) {
            list.add(toIndex, e);
            toIndex++;
        }
    }
}
