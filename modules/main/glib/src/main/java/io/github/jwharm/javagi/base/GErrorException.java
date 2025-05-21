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

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import org.gnome.glib.GError;
import org.jetbrains.annotations.ApiStatus;

import org.gnome.glib.Quark;

/**
 * A GErrorException is thrown when a GError is returned by native code. See
 * <a href="https://docs.gtk.org/glib/error-reporting.html">the Gtk
 * documentation on error reporting</a> for details about GError.
 */
public class GErrorException extends Exception {

    private final Quark domain;
    private final int code;
    private final String message;

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

    /*
     * Dereference the GError instance from the pointer.
     */
    private static GError dereference(MemorySegment pointer) {
        return new GError(pointer.get(ValueLayout.ADDRESS, 0));
    }
    
    /*
     * Get the message from the GError instance (used by the GErrorException
     * constructor).
     */
    private static String readMessage(MemorySegment pointer) {
        return dereference(pointer).readMessage();
    }

    /**
     * Create a GErrorException from a GError memory segment that was returned
     * by a native function.
     *
     * @param gerrorPtr pointer to a GError in native memory
     */
    @ApiStatus.Internal
    public GErrorException(MemorySegment gerrorPtr) {
        super(readMessage(gerrorPtr));
        GError gerror = dereference(gerrorPtr);
        this.domain = gerror.readDomain();
        this.code = gerror.readCode();
        this.message = gerror.readMessage();
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
    public GErrorException(Quark domain,
                           int code,
                           String message,
                           Object... args) {
        super(message);
        this.domain = domain;
        this.code = code;
        this.message = message == null ? null : message.formatted(args);
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
     * GErrorException.
     *
     * @return a newly created GError instance
     */
    public GError toGError() {
        return new GError(domain, code, message);
    }

    /**
     * Create a new GError instance with the domain, code and message of this
     * GErrorException.
     *
     * @return a newly created GError instance
     * @deprecated see {@link #toGError()}
     */
    @Deprecated
    public GError getGError() {
        return toGError();
    }
}
