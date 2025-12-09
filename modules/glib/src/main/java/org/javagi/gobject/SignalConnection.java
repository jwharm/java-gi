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

import org.gnome.gobject.Closure;
import org.gnome.gobject.GObject;
import org.gnome.gobject.GObjects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.MemorySegment;

import static java.util.Objects.requireNonNull;

/**
 * Represents a signal connection. With a {@code SignalConnection} object, a
 * signal connection can be blocked, unblocked, and disconnected. It is also
 * possible to check if the signal is still connected.
 *
 * @param <T> the type of the signal
 */
@SuppressWarnings("unused")
@NullMarked
public class SignalConnection<T> {
    /*
     * "unused" warnings are disabled with @SuppressWarnings("unused") because:
     * - the type parameter <T> is unused, but it's useful information to have
     *   in the type signature
     * - the `closure` field is unused, but the closure is stored in this field
     *   to keep it alive by the SignalConnection instance.
     */

    private final GObject instance;
    private final int handlerId;
    private final @Nullable Closure closure;

    /**
     * Create a SignalConnection instance for the provided GObject instance and
     * handler ID.
     *
     * @param instance  the native memory address of the GObject instance
     * @param handlerId the handler ID of the signal
     */
    public SignalConnection(MemorySegment instance, int handlerId) {
        this.instance = (GObject) requireNonNull(InstanceCache.getForType(instance, GObject::new));
        this.handlerId = handlerId;
        this.closure = null;
    }

    /**
     * Create a SignalConnection instance for the provided GObject instance and
     * handler ID.
     *
     * @param instance  the native memory address of the GObject instance
     * @param handlerId the handler ID of the signal
     * @param closure   closure for the signal callback
     */
    public SignalConnection(MemorySegment instance, int handlerId, @Nullable Closure closure) {
        this.instance = (GObject) requireNonNull(InstanceCache.getForType(instance, GObject::new));
        this.handlerId = handlerId;
        this.closure = closure;
    }

    /**
     * Blocks a handler of an instance so it will not be called during any
     * signal emissions unless it is unblocked again. Thus "blocking" a
     * signal handler means to temporarily deactivate it, a signal handler
     * has to be unblocked exactly the same amount of times it has been
     * blocked before to become active again.
     */
    public void block() {
        GObjects.signalHandlerBlock(instance, handlerId);
    }

    /**
     * Undoes the effect of a previous {@link #block()} call.  A blocked handler
     * is skipped during signal emissions and will not be invoked, unblocking it
     * (for exactly the amount of times it has been blocked before) reverts its
     * "blocked" state, so the handler will be recognized by the signal system
     * and is called upon future or currently ongoing signal emissions (since
     * the order in which handlers are called during signal emissions is
     * deterministic, whether the unblocked handler in question is called as
     * part of a currently ongoing emission depends on how far that emission has
     * proceeded yet).
     */
    public void unblock() {
        GObjects.signalHandlerUnblock(instance, handlerId);
    }

    /**
     * Disconnects a handler from an instance so it will not be called during
     * any future or currently ongoing emissions of the signal it has been
     * connected to. The {@code handlerId} becomes invalid and may be reused.
     */
    public void disconnect() {
        GObjects.signalHandlerDisconnect(instance, handlerId);
    }

    /**
     * Returns whether this signal is connected.
     *
     * @return whether the {@code handlerId} of this signal identifies a
     *         handler connected to the instance.
     */
    public boolean isConnected() {
        return GObjects.signalHandlerIsConnected(instance, handlerId);
    }
}
