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

package io.github.jwharm.javagi.interop;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Utility functions to split an array of variadic arguments into a first
 * argument and a null-terminated array of remaining arguments.
 */
public class VarargsUtil {

    /**
     * Return the first array element.
     *
     * @param  array input array, can be {@code null}
     * @param  <T>   array element type
     * @return the first element, or {@code null} if the input array is
     *         {@code null} or empty
     */
    public static <T> @Nullable T first(@Nullable T @Nullable[] array) {
        return array == null || array.length == 0 ? null : array[0];
    }

    /**
     * Return all but the first array elements, terminated with a {@code null}.
     * For example, {@code [1, 2, 3]} returns {@code [2, 3, null]}.
     *
     * @param  array input array, can be {@code null}
     * @param  <T>   array element type
     * @return a new array of all elements except the first, terminated with a
     *         {@code null}, or {@code null} if the input array is {@code null}
     *         or empty
     */
    @SuppressWarnings("unchecked") // cast is safe because the array is empty
    public static <T> @Nullable T @Nullable[] rest(@Nullable T @Nullable[] array) {
        return array == null ? null
                : array.length == 0 ? (T[]) new Object[] {}
                : Arrays.copyOfRange(array, 1, array.length + 1);
    }
}
