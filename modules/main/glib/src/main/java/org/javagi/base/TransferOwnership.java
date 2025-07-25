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

package org.javagi.base;

/**
 * Types of ownership transfer used by GObject-Introspection for many elements,
 * for example, a returned value.
 */
public enum TransferOwnership {
    /**
     * The recipient does not own the value
     */
    NONE,

    /**
     * The recipient owns the container but not the values
     */
    CONTAINER,

    /**
     * The recipient owns the values but not the container
     */
    VALUES,

    /**
     * The recipient owns the entire value
     */
    FULL
}
