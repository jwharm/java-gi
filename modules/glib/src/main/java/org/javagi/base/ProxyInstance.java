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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.MemorySegment;
import java.util.Objects;

/**
 * Base type for a Java proxy object to an instance in native memory.
 */
@NullMarked
public class ProxyInstance implements Proxy {

    /**
     * The native memory address
     */
    private MemorySegment address;

    /**
     * Create a new {@code ProxyInstance} object for an instance in native
     * memory.
     *
     * @param address the memory address of the instance
     */
    public ProxyInstance(MemorySegment address) {
        this.address = address;
    }

    /**
     * Get the memory address of the instance.
     *
     * @return the memory address of the instance
     */
    @Override
    public MemorySegment handle() {
        return address;
    }

    /**
     * Compare two proxy instances for equality. This will compare both the type
     * of the instances, and their memory addresses.
     *
     * @param  o the proxy instance to compare
     * @return whether the instances are equal
     */
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProxyInstance that = (ProxyInstance) o;
        return Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(address);
    }
}
