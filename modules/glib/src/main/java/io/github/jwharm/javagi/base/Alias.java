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

package io.github.jwharm.javagi.base;

import java.lang.foreign.MemorySegment;

/**
 * Base class for type aliases of primitive values.
 *
 * @param <T> The primitive value type
 */
public abstract class Alias<T> {

    private T value;

    /**
     * Create a new alias with the provided value.
     *
     * @param value the initial value of the alias
     */
    public Alias(T value) {
        this.value = value;
    }

    /**
     * Set the alias to the provided value, overwriting any existing value.
     *
     * @param value the new value
     */
    public void setValue(T value) {
        this.value = value;
    }

    /**
     * Get the current value of the alias.
     *
     * @return the current value, or {@code null} if the value has not been set
     */
    public T getValue() {
        return this.value;
    }

    /**
     * Check if the values of this alias equals the value of the provided alias.
     *
     * @param  other another Alias instance
     * @return true if the value of this Alias equals the value of the provided
     *         alias
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof Alias<?> alias
                && this.value.equals(alias.value);
    }

    /**
     * Get the hashcode of the value.
     *
     * @return hashcode of the value
     */
    @Override
    public int hashCode() {
        return value.hashCode();
    }

    /**
     * Convert an array of MemorySegment Aliases into an array of
     * MemorySegments.
     *
     * @param  array the array of Alias objects
     * @return an array of MemorySegments
     */
    public static MemorySegment[] getAddressValues(Alias<MemorySegment>[] array) {
        MemorySegment[] values = new MemorySegment[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }

    /**
     * Convert an array of Boolean Aliases into an array of booleans.
     *
     * @param  array the array of Alias objects
     * @return an array of booleans
     */
    public static boolean[] getBooleanValues(Alias<Boolean>[] array) {
        boolean[] values = new boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }

    /**
     * Convert an array of Byte Aliases into an array of bytes.
     *
     * @param  array the array of Alias objects
     * @return an array of bytes
     */
    public static byte[] getByteValues(Alias<Byte>[] array) {
        byte[] values = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }

    /**
     * Convert an array of Character Aliases into an array of chars.
     *
     * @param  array the array of Alias objects
     * @return an array of chars
     */
    public static char[] getCharacterValues(Alias<Character>[] array) {
        char[] values = new char[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }

    /**
     * Convert an array of Double Aliases into an array of doubles.
     *
     * @param  array the array of Alias objects
     * @return an array of doubles
     */
    public static double[] getDoubleValues(Alias<Double>[] array) {
        double[] values = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }

    /**
     * Convert an array of Float Aliases into an array of floats.
     *
     * @param  array the array of Alias objects
     * @return an array of floats
     */
    public static float[] getFloatValues(Alias<Float>[] array) {
        float[] values = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }

    /**
     * Convert an array of Integer Aliases into an array of integers.
     *
     * @param array the array of Alias objects
     * @return an array of integers.
     */
    public static int[] getIntegerValues(Alias<Integer>[] array) {
        int[] values = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }

    /**
     * Convert an array of Long Aliases into an array of longs.
     *
     * @param  array the array of Alias objects
     * @return an array of longs
     */
    public static long[] getLongValues(Alias<Long>[] array) {
        long[] values = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }

    /**
     * Convert an array of Short Aliases into an array of shorts.
     *
     * @param array the array of Alias objects
     * @return an array of shorts
     */
    public static short[] getShortValues(Alias<Short>[] array) {
        short[] values = new short[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }
}
