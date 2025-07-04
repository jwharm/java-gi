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

package org.javagi.base;

import org.javagi.base.Constants;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;

/**
 * Utility class to call {@link GLib#log} for debug purposes.
 */
public final class GLibLogger {

    // Prevent instantiation
    private GLibLogger() {}

    /**
     * Call {@link GLib#log} with log domain "java-gi" and level
     * {@link LogLevelFlags#LEVEL_DEBUG}.
     *
     * @param message the message format. See the {@code printf()}
     *                documentation
     * @param varargs the parameters to insert into the format string
     */
    public static void debug(String message, Object... varargs) {
        GLib.log(Constants.LOG_DOMAIN,
                LogLevelFlags.LEVEL_DEBUG,
                message,
                varargs);
    }
}
