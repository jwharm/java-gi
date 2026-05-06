/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2026 Jan-Willem Harmannij
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

package org.javagi.glib;

import org.javagi.base.Out;
import org.javagi.interop.Interop;
import org.gnome.glib.GLib;
import org.gnome.glib.HashTable;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

public class ArrayTest {

    /**
     * Test reading values from a GPtrArray
     */
    @Test
    void testArray() {
        try (var arena = Arena.ofConfined()) {
            var table = HashTable.new_(GLib::strHash, GLib::strEqual);
            for (int i = 0; i < 3; i++)
                table.insert(
                        Interop.allocate("key" + i, arena),
                        Interop.allocate("val" + i, arena));
            var values = table.getValuesAsPtrArray();
            assertNotNull(values);
            assertEquals(3, values.length);
            for (int i = 0; i < 3; i++) {
                String str = requireNonNull(Interop.getString(values[i]));
                assertEquals(4, str.length());
                assertTrue(str.startsWith("val"));
            }
        }
    }

    /**
     * Test writing to and reading from an inout array parameter
     */
    @Test
    void testArrayInOut() {
        String input = "c3RyaW5nIHRvIGRlY29kZQ==";
        String base64Decoded = "string to decode";

        Out<byte[]> bytesOut = new Out<>();
        bytesOut.set(input.getBytes(StandardCharsets.UTF_8));
        GLib.base64DecodeInplace(bytesOut);
        String decodedString = new String(requireNonNull(bytesOut.get()));
        assertEquals(base64Decoded, decodedString);
    }
}
