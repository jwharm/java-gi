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

import io.github.jwharm.javagi.interop.MemoryCleaner;

import java.lang.foreign.MemorySegment;

/**
 * Base class for ProxyInstances that will call a user-specified {@code free()}
 * function upon garbage-collection of the Java object.
 */
public class ManagedInstance extends ProxyInstance {

    /**
     * Create a new {@code ManagedInstance} object for an instance in native
     * memory, and register it in the {@link MemoryCleaner} so it will be
     * automatically cleaned during garbage collection.
     *
     * @param address the memory address of the instance
     */
    public ManagedInstance(MemorySegment address) {
        super(address);
        MemoryCleaner.register(this);
    }
}
