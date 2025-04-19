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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * A common interface to create a function pointer in native memory for a Java
 * functional interface. All Java-GI callbacks extend from this interface.
 */
public interface FunctionPointer {

    /**
     * Create a function pointer in native memory for this callback.
     *
     * @param  arena the function pointer will be allocated in this arena
     * @return the newly created function pointer
     */
    MemorySegment toCallback(Arena arena);
}
