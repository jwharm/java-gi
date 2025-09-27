/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2025 Jan-Willem Harmannij
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

package org.javagi.regress;

import org.gnome.gi.regress.*;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.util.Arrays;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestArrayFixedSize {
    @Test
    void struct() {
        var struct = new TestStructFixedArray();
        struct.frob();
        assertEquals(7, struct.readJustInt());

        // Read array
        assertArrayEquals(new int[] {42, 43, 44, 45, 46, 47, 48, 49, 50, 51}, struct.readArray());

        try (Arena arena = Arena.ofConfined()) {
            // Write a new array and read it back
            struct.writeArray(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, arena);
            assertArrayEquals(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, struct.readArray());

            // Write a null array and read it back
            struct.writeArray(null, arena);
            assertArrayEquals(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, struct.readArray());
        }
    }

    @Test
    void int_() {
        int[] correctSize = new int[32];
        Arrays.fill(correctSize, 42);
        hasParameterNamedAttrs(0, correctSize);

        // Test with too small array
        int[] wrongSize = new int[31];
        Arrays.fill(wrongSize, 42);
        assertThrows(IllegalArgumentException.class, () -> hasParameterNamedAttrs(0, wrongSize));
    }

    @Test
    void fixedSizeAndZeroTerminated() {
        // LikeXklConfigItem.name is a fixed size string of 32 bytes.
        // All unused bytes are filled with nulls.
        var x = new LikeXklConfigItem();

        String foo = "foo";
        x.setName(foo);

        // Prepare a byte string with "foo" and 29 nulls
        byte[] fooBytes = new byte[32];
        Arrays.fill(fooBytes, (byte) 0);
        for (int i = 0; i < foo.length(); i++)
            fooBytes[i] = (byte) foo.codePointAt(i);

        String str = new String(x.readName());
        assertArrayEquals(fooBytes, str.getBytes());

        String stars = "*".repeat(33);
        x.setName(stars);

        // Prepare a byte string with 31 stars and one null
        byte[] starBytes = new byte[32];
        Arrays.fill(starBytes, (byte) '*');
        starBytes[31] = (byte) 0;

        str = new String(x.readName());
        assertArrayEquals(starBytes, str.getBytes());
    }
}
