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

package org.javagi.gio;

import org.javagi.base.GErrorException;
import org.gnome.glib.GError;
import org.gnome.gio.Cancellable;
import org.gnome.gio.IOStream;
import org.gnome.gio.InputStream;
import org.gnome.gio.OutputStream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * An {@link java.lang.AutoCloseable} interface for GIO streams.
 * <p>
 * This interface extends {@link java.lang.AutoCloseable} and implements
 * {@link #close()} with a {@code default} method that calls
 * {@link #close(Cancellable)}. This interface is implemented by GIO streams
 * ({@link IOStream}, {@link InputStream} and {@link OutputStream}) so they
 * become {@code AutoCloseable} and can be used in a try-with-resources block.
 */
@NullMarked
public interface AutoCloseable extends java.lang.AutoCloseable {

    /**
     * A default implementation of {@link java.lang.AutoCloseable} that calls
     * {@link #close(Cancellable)}. The return value of
     * {@link #close(Cancellable)} is ignored.
     *
     * @throws java.io.IOException An {@code IOException} that is wrapped
     *                             around the {@link GErrorException} that is
     *                             thrown by {@link #close(Cancellable)}.
     */
    default void close() throws IOException {
        try {
            close(null);
        } catch (GErrorException gerror) {
            throw new IOException(gerror);
        }
    }

    /**
     * The {@code close} method that is implemented by GIO streams
     * ({@link IOStream}, {@link InputStream} and {@link OutputStream}).
     *
     * @param  cancellable optional {@link Cancellable} object, {@code null} to
     *                     ignore
     * @return {@code true} on success, {@code false} on failure
     * @throws GErrorException See {@link GError}
     */
    boolean close(@Nullable Cancellable cancellable)
            throws GErrorException;
}
