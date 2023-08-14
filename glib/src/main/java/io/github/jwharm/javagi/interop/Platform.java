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

package io.github.jwharm.javagi.interop;

import io.github.jwharm.javagi.base.UnsupportedPlatformException;

import java.util.StringJoiner;

/**
 * The Platform class provides utility functions to retrieve the runtime platform and
 * and check if a function is supported on the runtime platform.
 */
public class Platform {

    private static String runtimePlatform = null;

    /**
     * Determine the runtime platform
     * @return the runtime platform: "windows", "linux" or "macos"
     */
    public static String getRuntimePlatform() {
        if (runtimePlatform == null) {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("windows")) {
                runtimePlatform = "windows";
            } else if (osName.contains("linux")) {
                runtimePlatform = "linux";
            } else {
                runtimePlatform = "macos";
            }
        }
        return runtimePlatform;
    }

    /**
     * Check if the runtime platform is in the list of provided platforms; if not, throws UnsupportedPlatformException
     * @param supportedPlatforms the platforms on which the api call is supported
     * @throws UnsupportedPlatformException when the runtime platform does not support this api call
     */
    public static void checkSupportedPlatform(String... supportedPlatforms) throws UnsupportedPlatformException {
        // Check if the runtime platform is in the list of platforms where this function is available
        for (var platform : supportedPlatforms) {
            if (platform.equals(getRuntimePlatform())) {
                return;
            }
        }
        // Runtime platform is not in list of supported platforms: throw UnsupportedPlatformException
        StringJoiner joiner = new StringJoiner(" and ", "Unsupported API call (only available on ", ".");
        for (var platform : supportedPlatforms) {
            joiner.add(platform);
        }
        throw new UnsupportedPlatformException(joiner.toString());
    }
}
