/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 the Java-GI developers
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

package io.github.jwharm.javagi.gir;

public enum Scope {
    /**
     * Valid until the GDestroyNotify argument is called. Can be called multiple times before the GDestroyNotify is
     * called.
     */
    NOTIFIED, // "notified"
    /**
     * Only valid for the duration of the first callback invocation. Can only be called once.
     */
    ASYNC,    // "async"
    /**
     * Only valid for the duration of the call. Can be called multiple times during the call.
     */
    CALL,     // "call"
    /**
     * Valid until the process terminates.
     */
    FOREVER,  // "forever"
    /**
     * Default scope when not specified: bound to instance lifetime
     */
    BOUND;

    public static Scope from(String value) {
        if (value == null || value.isEmpty()) {
            return BOUND;
        }
        try {
            return Scope.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BOUND;
        }
    }

    /**
     * Get the Scope of a value:
     * <ul>
     * <li>Field: Always {@link #CALL}
     * <li>Parameter: The value of the {@code "scope"} attribute if present, else {@link #BOUND}, except for
     * constructor and function parameters: in that case {@link #FOREVER}.
     * </ul>
     *
     * @param value the value to get the scope for
     * @return the scope as described above
     */
    public static Scope ofTypedValue(TypedValue value) {
        var scope = value instanceof Parameter p ? p.scope()
                : value instanceof Field ? Scope.CALL
                : null;

        if (scope == Scope.BOUND) {
            var method = value.parent().parent();
            if (method instanceof Constructor || method instanceof Function)
                scope = Scope.FOREVER;
        }
        return scope;
    }
}
