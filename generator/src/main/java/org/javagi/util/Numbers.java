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

import java.util.Set;

/**
 * Utility class for parsing numbers where it is unknown whether they are signed
 */
public class Numbers {
    public static Byte parseByte(String str) throws NumberFormatException {
        String s = stripSuffix(str);
        return negative(s) ? Byte.parseByte(s) : (byte) Integer.parseInt(s);
    }

    public static Short parseShort(String str) throws NumberFormatException {
        String s = stripSuffix(str);
        return negative(s) ? Short.parseShort(s) : (short) Integer.parseInt(s);
    }

    public static Integer parseInt(String str) throws NumberFormatException {
        String s = stripSuffix(str);
        return negative(s) ? Integer.parseInt(s) : Integer.parseUnsignedInt(s);
    }

    public static Long parseLong(String str) throws NumberFormatException {
        String s = stripSuffix(str);
        return negative(s) ? Long.parseLong(s) : Long.parseUnsignedLong(s);
    }

    private static String stripSuffix(String str) {
        // suffixes for double, unsigned, long and float
        Set<Character> suffixes = Set.of('D', 'U', 'L', 'F');
        String result = str;
        while (suffixes.contains(result.charAt(Character.toUpperCase(result.length() - 1))))
            result = result.substring(0, result.length() - 1);
        return result;
    }

    private static boolean negative(String s) {
        return !s.isEmpty() && s.charAt(0) == '-';
    }
}
