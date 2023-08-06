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

package io.github.jwharm.javagi.pointer;

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
