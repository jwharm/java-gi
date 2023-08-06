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

/**
 * Base class of enumeration objects
 */
public interface Enumeration {

    /**
     * Get the integer value of this enumeration
     * @return the integer value of this enumeration value
     */
    int getValue();

    /**
     * Compare this enumeration value with an integer
     * @param enumeration the integer to compare this enumeration value with
     * @return true when {@code this.value == enumeration}
     */
    default boolean equals(int enumeration) {
        return getValue() == enumeration;
    }

    /**
     * Compare this enumeration value with another enumeration value
     * @param enumeration another enumeration value
     * @return true when {@code this.value == enumeration.value}
     */
    default boolean equals(Enumeration enumeration) {
        return enumeration != null && this.getValue() == enumeration.getValue();
    }

    /**
     * Convenience function to transfer an array of Enumeration objects 
     * into an array of integer values.
     * @param array An array of Enumeration objects
     * @return an array of the integer values of the provided Enumeration objects
     */
    static int[] getValues(Enumeration[] array) {
        int[] values = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].getValue();
        }
        return values;
    }
}
