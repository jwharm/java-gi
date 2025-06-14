/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 the Java-GI developers
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

package org.javagi.util;

import com.squareup.javapoet.CodeBlock;
import org.javagi.configuration.ClassNames;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Constants to identify supported platforms
 */
public class Platform {

    public static boolean GENERATE_PLATFORM_CHECKS = true;

    public final static int LINUX = 1;
    public final static int WINDOWS = 1 << 1;
    public final static int MACOS = 1 << 2;
    public final static int ALL = LINUX | WINDOWS | MACOS;

    /**
     * Determine the runtime platform
     *
     * @return the runtime platform
     */
    public static int getRuntimePlatform() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("windows"))    return WINDOWS;
        else if (osName.contains("linux")) return LINUX;
        else                               return MACOS;
    }

    /**
     * Generate a String representation of the specified (combination of)
     * platform(s)
     */
    public static String toString(int platform) {
        if (platform == ALL) return "linux, windows or macos";
        var joiner = new StringJoiner(" or ");
        if ((platform & LINUX) > 0)   joiner.add("linux");
        if ((platform & WINDOWS) > 0) joiner.add("windows");
        if ((platform & MACOS) > 0)   joiner.add("macos");
        return joiner.toString();
    }

    /**
     * Generate a String representation of the specified (combination of)
     * platform(s)
     */
    public static CodeBlock generateSupportCheck(int platform) {
        var joiner = new StringJoiner(", ");
        if ((platform & LINUX) > 0)   joiner.add("$1T.LINUX");
        if ((platform & WINDOWS) > 0) joiner.add("$1T.WINDOWS");
        if ((platform & MACOS) > 0)   joiner.add("$1T.MACOS");
        var builder = CodeBlock.builder();
        builder.add("$T.checkSupportedPlatform(", ClassNames.PLATFORM);
        builder.add(joiner.toString(), ClassNames.PLATFORM);
        builder.add(")");
        return builder.build();
    }

    public static List<Integer> toList(int platform) {
        List<Integer> list = new ArrayList<>(3);
        if ((platform & LINUX) > 0)   list.add(LINUX);
        if ((platform & WINDOWS) > 0) list.add(WINDOWS);
        if ((platform & MACOS) > 0)   list.add(MACOS);
        return list;
    }
}
