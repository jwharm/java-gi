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

/**
 * Utility class for parsing numbers where it is unknown whether they are signed
 */
public class Numbers {
    public static Byte parseByte(String s) throws NumberFormatException {
        return negative(s) ? Byte.parseByte(s) : (byte) Integer.parseInt(s);
    }

    public static Short parseShort(String s) throws NumberFormatException {
        return negative(s) ? Short.parseShort(s) : (short) Integer.parseInt(s);
    }

    public static Integer parseInt(String s) throws NumberFormatException {
        return negative(s) ? Integer.parseInt(s) : Integer.parseUnsignedInt(s);
    }

    public static Long parseLong(String s) throws NumberFormatException {
        return negative(s) ? Long.parseLong(s) : Long.parseUnsignedLong(s);
    }

    private static boolean negative(String s) {
        return !s.isEmpty() && s.charAt(0) == '-';
    }
}
