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
 * A pointer to a byte value.
 * Use {@code new PointerByte()} to create an instance, and
 * use {@link #get()} and {@link #set(Byte)} to get and set the value.
 */
public class PointerByte extends Pointer<Byte> {

    /**
     * Create the pointer. It does not point to a specific value.
     */
    public PointerByte() {
        super(ValueLayout.JAVA_BYTE);
    }

    /**
     * Create a pointer to an existing memory address.
     * @param address the memory address
     */
    public PointerByte(MemorySegment address) {
        super(address);
    }

    /**
     * Create the pointer and point it to the given initial value.
     * @param initialValue the initial value
     */
    public PointerByte(byte initialValue) {
        this();
        segment.set(ValueLayout.JAVA_BYTE, 0, initialValue);
    }

    /**
     * Use this method to set the value that the pointer points to.
     * @param value the new value that is pointed to
     */
    public void set(Byte value) {
        segment.set(ValueLayout.JAVA_BYTE, 0, value);
    }

    /**
     * Use this method to retrieve the value of the pointer.
     * @return The value of the pointer
     */
    public Byte get() {
        return get(0);
    }

    /**
     * Treat the pointer as an array, and return the given element.
     * <strong>Warning: There is no bounds checking.</strong>
     * @param index the array index
     * @return the value stored at the given index
     */
    public Byte get(int index) {
        return segment.get(ValueLayout.JAVA_BYTE, index);
    }
}
