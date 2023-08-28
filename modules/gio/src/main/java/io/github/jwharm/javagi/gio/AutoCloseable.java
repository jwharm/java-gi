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

package io.github.jwharm.javagi.gio;

import io.github.jwharm.javagi.base.GErrorException;
import org.gnome.gio.Cancellable;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * An {@link java.lang.AutoCloseable} interface for GIO streams.
 * <p>
 * This interface extends {@link java.lang.AutoCloseable} and implements the {@link #close()}
 * with a {@code default} version that calls the {@link #close(Cancellable)} method. This method
 * is implemented by GIO streams ({@link org.gnome.gio.IOStream}, {@link org.gnome.gio.InputStream}
 * and {@link org.gnome.gio.OutputStream}) so they become {@code AutoCloseable} and can be used in
 * a try-with-resources block.
 */
public interface AutoCloseable extends java.lang.AutoCloseable {

    /**
     * A default implementation of {@link java.lang.AutoCloseable} that calls {@link #close(Cancellable)}.
     * The return value of {@link #close(Cancellable)} is ignored.
     * @throws java.io.IOException An {@code IOException} that is wrapped around the {@link GErrorException}
     *                             that is thrown by {@link #close(Cancellable)}.
     */
    default void close() throws java.io.IOException {
        try {
            close(null);
        } catch (GErrorException gerror) {
            throw new IOException(gerror);
        }
    }

    /**
     * The {@code close} method that is implemented by GLib streams ({@link org.gnome.gio.IOStream},
     * {@link org.gnome.gio.InputStream} and {@link org.gnome.gio.OutputStream}).
     * @param cancellable optional {@link Cancellable} object, {@code null} to ignore
     * @return {@code true} on success, {@code false} on failure
     * @throws GErrorException See {@link org.gnome.glib.GError}
     */
    boolean close(@Nullable org.gnome.gio.Cancellable cancellable) throws GErrorException;
}