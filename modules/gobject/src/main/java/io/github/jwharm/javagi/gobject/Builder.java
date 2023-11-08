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
import java.util.ArrayList;

/**
 * Base class for all inner {@code Builder} classes inside GObject proxy classes.
 * @param <S> the type of the Builder that is returned
 */
@SuppressWarnings("rawtypes")
public abstract class Builder<S extends Builder> implements BuilderInterface {

    /**
     * Memory scope of allocated gvalues. It will be closed after the enclosing
     * builder instance has been built.
     */
    private final Arena arena = Arena.ofConfined();

    /**
     * List of all property names that are set
     */
    private final ArrayList<String> names = new ArrayList<>();
    
    /**
     * List of all property values that are set
     */
    private final ArrayList<Value> values = new ArrayList<>();

    /**
     * Get the arena for allocating memory in this builder
     * @return the arena for allocating memory in this builder
     */
    @Override
    public Arena getArena() {
        return arena;
    }

    /**
     * Add the provided property name and value to the builder
     * @param name name of the property
     * @param value value of the property (a {@code GValue})
     */
    @Override
    public void addBuilderProperty(String name, Value value) {
        names.add(name);
        values.add(value);
    }

    /**
     * Get the property names
     * @return a {@code String} array of property names
     */
    public String[] getNames() {
        return names.toArray(new String[0]);
    }

    /**
     * Get the property values
     * @return a {@code GValue} array of property names
     */
    public Value[] getValues() {
        return values.toArray(new Value[0]);
    }

}
