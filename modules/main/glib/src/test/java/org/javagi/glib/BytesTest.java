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

package org.javagi.glib;

import org.gnome.glib.ChecksumType;
import org.gnome.glib.GLib;
import org.javagi.base.GErrorException;
import org.gnome.glib.KeyFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BytesTest {

    @Test
    public void testFromGBytes() {
        var expected = "e2fc714c4727ee9395f324cd2e7f331f";
        var computed = GLib.computeChecksumForBytes(ChecksumType.MD5, "abcd".getBytes());
        assertEquals(expected, computed);
    }

    @Test
    public void testToGBytes() throws GErrorException {
        byte[] data = """
            [MyGroup]
            FirstKey=Hello
            SecondKey=Goodbye
            """.getBytes();
        var keyFile = new KeyFile();
        var success = keyFile.loadFromBytes(data);
        assertTrue(success);
        assertEquals("MyGroup", keyFile.getStartGroup());
    }
}
