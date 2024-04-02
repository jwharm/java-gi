/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.base;

/**
 * Base class for bitfield objects.
 */
public abstract class Bitfield {

    private final int value;

    /**
     * Create a bitfield with the provided integer value.
     *
     * @param value the initial value of the bitfield
     */
    public Bitfield(int value) {
        this.value = value;
    }

    /**
     * Get the value of the bitfield as an integer value.
     *
     * @return the value of the bitfield
     */
    public int getValue() {
        return this.value;
    }

    /**
     * Compare the value of this bitfield with the provided int value.
     *
     * @param  bitfield an int value to compare with
     * @return true when {@code this.value == bitfield}
     */
    public boolean equals(int bitfield) {
        return this.value == bitfield;
    }

    /**
     * Compare the value of this bitfield with the value of the provided
     * bitfield.
     *
     * @param  mask another bitfield
     * @return true when {@code this.value == mask.value}
     */
    public boolean equals(Bitfield mask) {
        return this.value == mask.value;
    }

    public boolean test(Bitfield other) {
        if (other == null)
            return false;

        if (!this.getClass().equals(other.getClass()))
            return false;

        return (getValue() & other.getValue()) == other.getValue();
    }

    /**
     * If the provided object is a {@link Bitfield} instance, the values are
     * compared. If the provided object is an {@link Integer}, the value is
     * compared to it. Otherwise, false is returned.
     *
     * @param  other the object to compare the bitfield to
     * @return whether the value of the bitfield is equal to the provided value
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof Bitfield mask)
            return equals(mask);

        if (other instanceof Integer bitfield)
            return equals(bitfield.intValue());

        return false;
    }

    /**
     * Get the (integer) value of the bitfield.
     *
     * @return the value of the bitfield
     */
    @Override
    public int hashCode() {
        return value;
    }

    /**
     * Get a String representation of the bitfield.
     *
     * @return a String {@code "ClassName [value]"}
     */
    @Override
    public String toString() {
        return "%s [%d]".formatted(getClass().getName(), value);
    }

    /**
     * Convert an array of Bitfield objects into an array of integers.
     *
     * @param  array Array of Bitfield objects
     * @return array of integers
     */
    public static int[] getValues(Bitfield[] array) {
        int[] values = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }
}
