/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 Jan-Willem Harmannij
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

package org.javagi.gio;

import org.gnome.gobject.GObject;
import org.gnome.gio.ListModel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.*;

/**
 * This interface is implemented by {@link ListModel}, so it can be used like a
 * regular Java {@link List}.
 * <p>
 * Because {@code ListModel} only defines operations to retrieve items and
 * size, the default implementations of the {@code List} mutator methods
 * throw {@code UnsupportedOperationException}. ListModel implementations that
 * support mutation must override the following methods:
 * <ul>
 *     <li>{@link #set(int, E)}
 *     <li>{@link #add(E)}
 *     <li>{@link #add(int, E)}
 *     <li>{@link #remove(int)}
 * </ul>
 * It is recommended to also override other operations such as {@link #clear()}
 * with a more efficient implementation.
 *
 * @param <E> The item type must be a GObject.
 */
public interface ListModelJavaList<E extends GObject> extends List<E> {

    int getNItems();
    E getItem(int position);

    /**
     * {@inheritDoc}
     *
     * @apiNote This operation is implemented with
     *          {@link ListModel#getNItems()}.
     */
    @Override
    default int size() {
        return getNItems();
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This operation is implemented by checking if {@link #size}
     *          returns 0.
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
    @Override
    default @NotNull Iterator<E> iterator() {
        return listIterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default Object @NotNull [] toArray() {
        return toArray(new Object[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked") // Unchecked casts are unavoidable here
    default <T> T @NotNull [] toArray(T @NotNull [] a) {
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
     * {@inheritDoc}
     *
     * @apiNote This operation is implemented by calling {@link #remove(int)} on the result of {@link #indexOf(Object)}.
     */
    @Override
    default boolean remove(Object o) {
        int index = indexOf(o);
        if (index < 0)
            return false;
        remove(index);
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This operation is implemented by calling
     *          {@link #contains(Object)} for all elements in the specified
     *          collection.
     */
    @Override
    default boolean containsAll(@NotNull Collection<?> c) {
        for (Object item : c)
            if (! contains(item))
                return false;
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This operation is implemented by calling {@link #add(E)}
     *          for all elements in the specified collection.
     */
    @Override
    default boolean addAll(@NotNull Collection<? extends E> c) {
        boolean changed = false;
        for (E item : c) {
            add(item);
            changed = true;
        }
        return changed;
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This operation is implemented by calling {@link #add(int, E)}
     *          for all elements in the specified collection.
     */
    @Override
    default boolean addAll(int index, @NotNull Collection<? extends E> c) {
        boolean changed = false;
        for (E item : c) {
            add(index++, item);
            changed = true;
        }
        return changed;
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This operation is implemented with {@link #remove(Object)} for
     *          all elements that are in the specified collection.
     */
    @Override
    default boolean removeAll(@NotNull Collection<?> c) {
        boolean changed = false;
        for (Object item : c) {
            if (remove(item))
                changed = true;
        }
        return changed;
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This operation is implemented with {@link #remove(Object)} for
     *          all elements that are not in the specified collection.
     */
    @Override
    default boolean retainAll(@NotNull Collection<?> c) {
        boolean changed = false;
        for (E item : this) {
            if (!c.contains(item))
                changed = remove(item);
        }
        return changed;
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This operation is implemented by repeatedly calling
     *          {@link #remove(int)} until the list is empty.
     */
    @Override
    default void clear() {
        int size = size();
        for (int i = 0; i < size; i++)
            remove(0);
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
    @Override
    default @NotNull ListIterator<E> listIterator() {
        return listIterator(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default @NotNull ListIterator<E> listIterator(int index) {
        return new ListIterator<>() {
            int next = index; // Index of the next element to be returned
            int last = -1;    // Index of the last returned element

            @Override
            public boolean hasNext() {
                return next < size();
            }

            @Override
            public E next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                last = next;
                return get(next++);
            }

            @Override
            public boolean hasPrevious() {
                return next > 0;
            }

            @Override
            public E previous() {
                if (!hasPrevious())
                    throw new NoSuchElementException();
                last = --next;
                return get(last);
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
                if (last < 0)
                    throw new IllegalStateException();
                ListModelJavaList.this.remove(last);
                if (last < next) // Adjust 'next' if remove was before it
                    next--;
                last = -1;       // Reset 'last'
            }

            @Override
            public void set(E e) {
                if (last < 0)
                    throw new IllegalStateException();
                ListModelJavaList.this.set(last, e);
            }

            @Override
            public void add(E e) {
                ListModelJavaList.this.add(next, e);
                next++;    // Adjust 'next' to reflect the added element
                last = -1; // Reset 'last'
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default @NotNull List<E> subList(int fromIndex, int toIndex) {
        return new SubList<>(this, fromIndex, toIndex);
    }

    @ApiStatus.Internal
    class SubList<E extends GObject, List extends ListModelJavaList<E>> implements ListModelJavaList<E> {
        protected final List list;
        protected final int fromIndex;
        protected int toIndex;

        public SubList(List list, int fromIndex, int toIndex) {
            if (fromIndex < 0 || fromIndex > toIndex || toIndex > list.size())
                throw new IndexOutOfBoundsException();
            this.list = Objects.requireNonNull(list);
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }

        @Override
        public int getNItems() {
            return toIndex - fromIndex;
        }

        @Override
        public E getItem(int position) {
            if (position < 0 || position >= size())
                throw new IndexOutOfBoundsException();
            return list.getItem(position + fromIndex);
        }
    }
}
