/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2026 Jan-Willem Harmannij
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

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import org.gnome.glib.GError;

import org.gnome.glib.Quark;
import org.javagi.interop.MemoryCleaner;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A GErrorException is thrown when a GError is returned by native code. See
 * <a href="https://docs.gtk.org/glib/error-reporting.html">the Gtk
 * documentation on error reporting</a> for details about GError.
 */
@NullMarked
public class GErrorException extends Exception {
    private final Quark domain;
    private final int code;

    /**
     * Check if an error is set.
     *
     * @param  gerrorPtr pointer to a GError in native memory
     * @return true when an error was set on this pointer
     */
    public static boolean isErrorSet(MemorySegment gerrorPtr) {
        MemorySegment gerror = gerrorPtr.get(ValueLayout.ADDRESS, 0);
        return (! gerror.equals(MemorySegment.NULL));
    }

    /**
     * Create a GErrorException from a GError memory location that was returned
     * by a native function. Consumes (frees) the GError.
     *
     * @param gerrorPtr pointer to a GError in native memory
     * @return the newly created GErrorException
     */
    public static GErrorException take(MemorySegment gerrorPtr) {
        GError err = new GError(gerrorPtr.get(ValueLayout.ADDRESS, 0));
        GErrorException exc = new GErrorException(err);
        err.free();
        return exc;
    }

    /**
     * Create a GErrorException from a GError memory location that was returned
     * by a native function. Consumes (frees) the GError.
     *
     * @param gerrorPtr pointer to a GError in native memory
     * @deprecated Use {@link #take} instead
     */
    @Deprecated
    public GErrorException(MemorySegment gerrorPtr) {
        GError err = new GError(gerrorPtr.get(ValueLayout.ADDRESS, 0));
        this(err);
        err.free();
    }

    /**
     * Create a GErrorException from a native GError.
     *
     * @param err a GError in native memory
     */
    public GErrorException(GError err) {
        this(err.readDomain(), err.readCode(), err.readMessage());
    }

    /**
     * Create a GErrorException that can be used to return a GError from a Java
     * callback function to native code. See
     * <a href="https://docs.gtk.org/glib/error-reporting.html">the Gtk
     * documentation on error reporting</a> for details.
     *
     * @param domain  the GError error domain
     * @param code    the GError error code
     * @param message the error message, printf-style formatted
     * @param args    varargs parameters for message format
     */
    public GErrorException(Quark domain, int code, @Nullable String message, @Nullable Object... args) {
        super(message == null ? null : message.formatted(args));
        this.domain = domain;
        this.code = code;
    }

    /**
     * Get the error code.
     *
     * @return the code of the GError
     */
    public int getCode() {
        return code;
    }

    /**
     * Get the error domain.
     *
     * @return The domain of the GError
     */
    public Quark getDomain() {
        return domain;
    }
    
    /**
     * Create a new GError instance with the domain, code and message of this
     * GErrorException. The instance will automatically be freed during GC.
     *
     * @return a newly created GError instance
     */
    public GError toGError() {
        return new GError(getDomain(), getCode(), getMessage());
    }

    /**
     * Create a new GError instance with the domain, code and message of this
     * GErrorException. The instance will <b>not</b> be automatically freed.
     *
     * @return a newly created GError instance
     */
    public GError toGErrorUnowned() {
        GError err = toGError();
        MemoryCleaner.yieldOwnership(err);
        return err;
    }
}
