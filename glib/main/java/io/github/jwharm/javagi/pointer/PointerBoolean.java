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

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * A pointer to a boolean value.
 * Use {@code new PointerBoolean()} to create an instance, and
 * use {@link #get()} and {@link #set(Boolean)} to get and set the value.
 */
public class PointerBoolean extends Pointer<Boolean> {

    /**
     * Create the pointer. It does not point to a specific value.
     */
    public PointerBoolean() {
        super(ValueLayout.JAVA_INT);
    }

    /**
     * Create a pointer to an existing memory address.
     * @param address the memory address
     */
    public PointerBoolean(MemorySegment address) {
        super(address);
    }

    /**
     * Create the pointer and point it to the given initial value.
     * @param initialValue the initial value
     */
    public PointerBoolean(boolean initialValue) {
        this();
        segment.set(ValueLayout.JAVA_INT, 0, initialValue ? 1 : 0);
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(Boolean value) {
        segment.set(ValueLayout.JAVA_INT, 0, value ? 1 : 0);
    }

    /**
     * Use this method to retrieve the value that the pointer points to.
     * @return the value of the pointer
     */
    public Boolean get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * <strong>Warning: There is no bounds checking.</strong>
     * @param index the array index
     * @return the value stored at the given index
     */
    public Boolean get(int index) {
        return segment.getAtIndex(ValueLayout.JAVA_INT, index) != 0;
    }
}
