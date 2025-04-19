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

package io.github.jwharm.javagi.interop;

/**
 * Thrown when an unexpected error occurs when calling a native function or
 * reading/writing from/to native memory.
 */
public class InteropException extends RuntimeException {

    /**
     * Create an InteropException that wraps another Throwable.
     *
     * @param cause the Throwable to wrap in the InteropException
     */
    public InteropException(Throwable cause) {
        super(cause);
    }

    /**
     * Create an InteropException with the provided message.
     *
     * @param message the exception message.
     */
    public InteropException(String message) {
        super(message);
    }
}
