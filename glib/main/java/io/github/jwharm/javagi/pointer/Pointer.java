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

import io.github.jwharm.javagi.base.Proxy;
import org.gnome.glib.GLib;

import java.lang.foreign.*;
import java.lang.reflect.Array;

/**
 * A Pointer object represents a pointer to a value or array, where native 
 * code expects a pointer and Java-GI cannot marshall it to something nicer.
 * For example, use {@code PointerInteger} for an {@code *int} pointer.
 * @param <T> The type of the value that the pointer refers to
 */
public abstract class Pointer<T> implements Proxy, Iterable<T> {

    /**
     * The memory address of the pointer
     */
    protected final MemorySegment segment;

    // If the segment is managed by the garbage collector
    private final boolean managedScope;

    /**
     * Allocate a new memory segment with the provided memory layout.
     * @param layout the memory layout
     */
    protected Pointer(ValueLayout layout) {
        this.segment = SegmentAllocator.nativeAllocator(SegmentScope.auto()).allocate(layout);
        this.managedScope = true;
    }

    /**
     * Instantiate a Pointer object that points to the provided address.
     * @param address The memory address
     */
    protected Pointer(MemorySegment address) {
        this.segment = address;
        this.managedScope = false;
    }

    /**
     * Return the memory address of the pointer
     * @return the memory address
     */
    public MemorySegment handle() {
        return segment;
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
     * Release the pointer. This will run {@link GLib#free(MemorySegment)}
     * on the native memory address.
     */
    public void free() {
        if (! managedScope) {
            GLib.free(segment);
        }
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
            this.free();
        }
        return array;
    }

}
