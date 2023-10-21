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

package io.github.jwharm.javagi.gobject;

import org.gnome.gobject.Value;

import java.lang.foreign.Arena;

/**
 * Base interface for nested Builder types in interfaces.
 */
public interface BuilderInterface {

    /**
     * Get the arena for allocating memory in this builder
     * @return the arena for allocating memory in this builder
     */
    Arena getArena();

    /**
     * Add the provided property name and value to the builder
     * @param name name of the property
     * @param value value of the property (a {@code GValue})
     */
    void addBuilderProperty(String name, Value value);
}
