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

import org.gnome.glib.*;
import org.javagi.base.GErrorException;
import org.javagi.interop.Interop;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test GHashTable wrapper class
 */
public class HashTableTest {

    @Test
    void testStringHashTable() {
        try {
            HashTable<String, String> hashTable = Uri.parseParams("name=john&age=41", -1, "&");
            assertEquals(2, hashTable.size());
            assertTrue(hashTable.contains("name"));
            assertTrue(hashTable.contains("age"));
            assertEquals("john", hashTable.get("name"));
            assertEquals("41", hashTable.lookup("age"));
        } catch (GErrorException e) {
            fail(e);
        }
    }

    @Test
    void testEnumHashTable() {
        var table1 = new HashTable<>(
                GLib::strHash,
                GLib::strEqual,
                Interop::getString,
                ChecksumType::of);
        table1.put("enum", ChecksumType.MD5);
        assertEquals(ChecksumType.MD5, table1.get("enum"));
    }

    @Test
    void testFlagsHashTable() {
        var set = Set.of(IOFlags.IS_READABLE, IOFlags.IS_WRITABLE);
        var table2 = new HashTable<>(
                GLib::strHash,
                GLib::strEqual,
                Interop::getString,
                IOFlags::of);
        table2.put("flags", set);
        assertEquals(set, table2.get("flags"));
    }
}
