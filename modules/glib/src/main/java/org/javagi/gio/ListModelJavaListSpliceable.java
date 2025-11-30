/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 the Java-GI developers
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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public interface ListModelJavaListSpliceable<E extends GObject> extends ListModelJavaListMutable<E> {

    void splice(int position, int nRemovals, @Nullable E[] additions);
    void splice(int position, int nRemovals, @NonNull Collection<? extends E> additions);

    /**
     * {@inheritDoc}
     *
     * @apiNote This implementation delegates to {@link #splice(int, int, E[])}.
     */
    @Override
    default void clear() {
        splice(0, size(), (E[]) null);
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This implementation delegates to {@link #splice(int, int, Collection)}.
     */
    @Override
    default E set(int index, E element) {
        return ListModelJavaListMutable.super.set(index, element);
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This implementation delegates to {@link #splice(int, int, Collection)}.
     */
    @Override
    default void add(int index, E element) {
        ListModelJavaListMutable.super.add(index, element);
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This implementation delegates to {@link #splice(int, int, Collection)}.
     */
    @Override
    default boolean addAll(@NonNull Collection<? extends E> c) {
        splice(size(), 0, Objects.requireNonNull(c));
        return !c.isEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This implementation delegates to {@link #splice(int, int, Collection)}.
     */
    @Override
    default boolean addAll(int index, @NonNull Collection<? extends E> c) {
        splice(index, 0, Objects.requireNonNull(c));
        return !c.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default @NonNull List<E> subList(int fromIndex, int toIndex) {
        return new SubList<>(this, fromIndex, toIndex);
    }

    class SubList<E extends GObject, List extends ListModelJavaListSpliceable<E>>
            extends ListModelJavaListMutable.SubList<E, List>
            implements ListModelJavaListSpliceable<E> {

        public SubList(List list, int fromIndex, int toIndex) {
            super(list, fromIndex, toIndex);
        }

        @Override
        public void splice(int position, int nRemovals, @Nullable E[] additions) {
            if (position < 0 || nRemovals < 0 || position + nRemovals > size())
                throw new IndexOutOfBoundsException();
            list.splice(position + fromIndex, nRemovals, additions);
            toIndex -= nRemovals;
            toIndex += additions == null ? 0 : additions.length;
        }

        @Override
        public void splice(int position, int nRemovals, @NonNull Collection<? extends E> additions) {
            if (position < 0 || nRemovals < 0 || position + nRemovals > size())
                throw new IndexOutOfBoundsException();
            list.splice(position + fromIndex, nRemovals, additions);
            toIndex -= nRemovals;
            toIndex += additions.size();
        }
    }
}
