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
 * A wrapper class for out-parameters of (usually primitive) values. When a
 * method expects an {@code Out<>} object, the user must create it, optionally
 * fill it with an initial value, and pass it to the method. After the method
 * has returned, the user can read the the value of the out-parameter with a
 * call to {@link #get()}.
 *
 * @param <T> The parameter type.
 */
public class Out<T> {
    
    private T value;
    
    /**
     * Create an Out object with no initial value.
     */
    public Out() {
    }
    
    /**
     * Create an Out object and set the initial value.
     *
     * @param value the initial value
     */
    public Out(T value) {
        this.value = value;
    }
    
    /**
     * Get the value from the out-parameter.
     *
     * @return the value of the out-parameter
     */
    public T get() {
        return value;
    }
    
    /**
     * Set the parameter to the provided value.
     *
     * @param value the value to set
     */
    public void set(T value) {
        this.value = value;
    }
}
