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

package org.javagi.gobject;

import org.javagi.base.FunctionPointer;
import org.javagi.gobject.types.Signals;
import org.javagi.interop.Arenas;
import org.javagi.interop.Interop;
import org.gnome.gobject.ConnectFlags;
import org.gnome.gobject.Value;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;

/**
 * Base class for all inner {@code Builder} classes inside GObject proxy
 * classes.
 *
 * @param <S> the type of the Builder that is returned
 */
@SuppressWarnings("rawtypes")
@NullMarked
public abstract class Builder<S extends Builder> implements BuilderInterface {

    /**
     * Represents a signal that must be connected after the object is
     * constructed. The name is the detailed name of the signal.
     */
    private record ConnectRequest(String name, FunctionPointer callback) {
    }

    /**
     * Memory scope of allocated GValues and strings. It will be closed after
     * the enclosing builder instance has been built.
     */
    private final Arena arena = Arena.ofConfined();

    /**
     * List of all property names that are set
     */
    private final ArrayList<String> names = new ArrayList<>();
    
    /**
     * List of all property values that are set
     */
    private final ArrayList<@Nullable Value> values = new ArrayList<>();

    /**
     * List of all signals that must be connected
     */
    private final ArrayList<ConnectRequest> connectRequests = new ArrayList<>();

    /**
     * Get the arena for allocating memory in this builder
     *
     * @return the arena for allocating memory in this builder
     */
    @Override
    public Arena getArena() {
        return arena;
    }

    /**
     * Add the provided property name and value to the builder
     *
     * @param name  name of the property
     * @param value value of the property (a {@code GValue})
     */
    @Override
    public void addBuilderProperty(String name, @Nullable Value value) {
        names.add(name);
        values.add(value);
    }

    /**
     * Add the provided signal to the builder
     *
     * @param name     the signal name
     * @param callback the signal callback
     */
    @Override
    public void connect(String name, FunctionPointer callback) {
        connectRequests.add(new ConnectRequest(name, callback));
    }

    /**
     * Add the provided detailed signal to the builder
     *
     * @param name     the signal name
     * @param detail   the signal detail
     * @param callback the signal callback
     */
    @Override
    public void connect(String name, @Nullable String detail, FunctionPointer callback) {
        boolean isDetailed = detail != null && !detail.isBlank();
        String fullName = name + (isDetailed ? ("::" + detail) : "");
        connectRequests.add(new ConnectRequest(fullName, callback));
    }

    /**
     * Connect the requested signals to the newly created object
     *
     * @param handle pointer to the newly created object
     */
    public void connectSignals(MemorySegment handle) {
        for (var s : connectRequests) {
            try {
                var _callbackArena = Arena.ofShared();
                var result = (long) Signals.g_signal_connect_data.invokeExact(
                        handle,
                        Interop.allocateNativeString(s.name, arena),
                        s.callback.toCallback(_callbackArena),
                        Arenas.cacheArena(_callbackArena),
                        Arenas.CLOSE_CB_SYM,
                        ConnectFlags.DEFAULT.getValue());
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
        }
    }

    /**
     * Get the property names
     *
     * @return a {@code String} array of property names
     */
    public String[] getNames() {
        return names.toArray(new String[0]);
    }

    /**
     * Get the property values
     *
     * @return a {@code GValue} array of property names
     */
    public Value @Nullable [] getValues() {
        return values.toArray(new Value[0]);
    }
}
