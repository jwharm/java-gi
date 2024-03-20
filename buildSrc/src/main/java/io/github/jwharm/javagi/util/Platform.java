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

package io.github.jwharm.javagi.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Constants to identify supported platforms
 */
public class Platform {

    public final static int LINUX = 1;
    public final static int WINDOWS = 1 << 1;
    public final static int MACOS = 1 << 2;
    public final static int ALL = LINUX | WINDOWS | MACOS;

    /**
     * Generate a String representation of the specified (combination of) platform(s)
     */
    public static String toString(int platform) {
        var joiner = new StringJoiner(", ");
        if ((platform & LINUX) > 0) joiner.add("linux");
        if ((platform & WINDOWS) > 0) joiner.add("windows");
        if ((platform & MACOS) > 0) joiner.add("macos");
        return joiner.toString();
    }

    /**
     * Generate a String representation of the specified (combination of) platform(s)
     */
    public static String toStringLiterals(int platform) {
        var joiner = new StringJoiner(", ");
        if ((platform & LINUX) > 0) joiner.add("\"linux\"");
        if ((platform & WINDOWS) > 0) joiner.add("\"windows\"");
        if ((platform & MACOS) > 0) joiner.add("\"macos\"");
        return joiner.toString();
    }

    public static List<Integer> toList(int platform) {
        List<Integer> list = new ArrayList<>(3);
        if ((platform & LINUX) > 0) list.add(LINUX);
        if ((platform & WINDOWS) > 0) list.add(WINDOWS);
        if ((platform & MACOS) > 0) list.add(MACOS);
        return list;
    }
}
