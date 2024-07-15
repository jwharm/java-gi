/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package io.github.jwharm.javagi.gio;

import org.gnome.gobject.GObject;
import org.gnome.gio.ListModel;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * This interface is implemented by {@link ListModel}, so it can be used like a
 * regular Java {@link List}. The list is immutable, so all mutations such as
 * {@link #add}, {@link #set} and {@link #remove} throw
 * {@link UnsupportedOperationException}.
 *
 * @param <E> The item type must be a GObject.
 */
public interface ListModelList<E extends GObject> extends List<E> {

    int getNItems();
    E getItem(int position);

    /**
     * {@inheritDoc}
     *
     * @apiNote This operation is implemented with {@link ListModel#getNItems()}
     */
    @Override
    default int size() {
        return getNItems();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default boolean contains(Object o) {
        if (o == null)
            return false;

        for (E item : this) {
            if (o.equals(item))
                return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    default Iterator<E> iterator() {
        return listIterator();
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    default Object @NotNull [] toArray() {
        return toArray(new Object[0]);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @SuppressWarnings("unchecked") // Unchecked casts are unavoidable here
    default <T> T @NotNull [] toArray(@NotNull T @NotNull [] a) {
        int size = size();
        T[] data = a.length >= size ? a :
                (T[]) Array.newInstance(a.getClass().getComponentType(), size);

        for (int i = 0; i < size(); i++)
            data[i] = (T) get(i);

        if (data.length > size)
            data[size] = null;

        return data;
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Override
    default boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Override
    default boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default boolean containsAll(@NotNull Collection<?> c) {
        for (Object item : c)
            if (! contains(item))
                return false;
        return true;
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Override
    default boolean addAll(@NotNull Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Override
    default boolean addAll(int index, @NotNull Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Override
    default boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Override
    default boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Override
    default void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This operation is implemented with {@link ListModel#getItem}
     */
    @Override
    default E get(int index) {
        return getItem(index);
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Override
    default E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Override
    default void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Override
    default E remove(int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default int indexOf(Object o) {
        for (int i = 0; i < size(); i++) {
            E item = get(i);
            if (o == null && item == null)
                return i;
            if (item.equals(o))
                return i;
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default int lastIndexOf(Object o) {
        for (int i = size() - 1; i >= 0; i--) {
            E item = get(i);
            if (o == null && item == null)
                return i;
            if (item.equals(o))
                return i;
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    default ListIterator<E> listIterator() {
        return listIterator(0);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    default ListIterator<E> listIterator(int index) {
        return new ListIterator<>() {
            int next = index;

            @Override
            public boolean hasNext() {
                return next < size();
            }

            @Override
            public E next() {
                return get(next++);
            }

            @Override
            public boolean hasPrevious() {
                return next > 0;
            }

            @Override
            public E previous() {
                return get(--next);
            }

            @Override
            public int nextIndex() {
                return next;
            }

            @Override
            public int previousIndex() {
                return next - 1;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(E e) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(E e) {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    default List<E> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > size() || fromIndex > toIndex)
            throw new IndexOutOfBoundsException();

        return new ListModelList<>() {
            @Override
            public int getNItems() {
                return toIndex - fromIndex;
            }

            @Override
            public E getItem(int position) {
                return ListModelList.this.getItem(position + fromIndex);
            }
        };
    }
}
