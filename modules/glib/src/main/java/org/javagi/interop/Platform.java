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

package org.javagi.interop;

/**
 * The Platform enum represents the runtime platform.
 */
public enum Platform {
    WINDOWS,
    LINUX,
    MACOS;

    private static Platform runtimePlatform = null;

    /**
     * Determine the runtime platform
     * @return the runtime platform: "windows", "linux" or "macos"
     */
    public static Platform getRuntimePlatform() {
        if (runtimePlatform == null) {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win"))
                runtimePlatform = WINDOWS;
            else if (osName.contains("nux"))
                runtimePlatform = LINUX;
            else if (osName.contains("mac") || osName.contains("darwin"))
                runtimePlatform = MACOS;
        }
        return runtimePlatform;
    }
}
