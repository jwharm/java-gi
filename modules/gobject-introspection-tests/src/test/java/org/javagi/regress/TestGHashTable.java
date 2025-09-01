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

import org.gnome.gi.regress.TestEnum;
import org.gnome.gi.regress.TestFlags;
import org.gnome.glib.GLib;
import org.gnome.glib.HashTable;
import org.gnome.gobject.Value;
import org.javagi.base.Out;
import org.javagi.gobject.ValueUtil;
import org.javagi.gobject.types.Types;
import org.javagi.interop.Interop;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.util.Map;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestGHashTable {
    private static final Map<String, String> EXPECTED_HASH = Map.of("baz", "bat", "foo", "bar", "qux", "quux");

    @Test
    void nullReturn() {
        assertNull(testGhashNullReturn());
    }

    @Test
    void noneReturn() {
        assertEquals(EXPECTED_HASH, testGhashNothingReturn());
        assertEquals(EXPECTED_HASH, testGhashNothingReturn2());
    }

    HashTable<String, Value> gvalueMap(Arena arena) {
        HashTable<String, Value> map = new HashTable<>(
                GLib::strHash,
                GLib::strEqual,
                Interop::getStringFrom,
                Value::new);

        Value v1 = new Value(arena);
        v1.init(Types.INT);
        v1.setInt(12);
        Value v2 = new Value(arena);
        v2.init(Types.BOOLEAN);
        v2.setBoolean(true);
        Value v3 = new Value(arena);
        v3.init(Types.STRING);
        v3.setString("some text");
        Value v4 = new Value(arena);
        v4.init(Types.STRV);
        v4.setBoxed(Interop.allocateNativeArray(new String[] {"first", "second", "third"}, true, arena));
        Value v5 = new Value(arena);
        v5.init(TestFlags.getType());
        v5.setFlags(TestFlags.FLAG1.getValue() | TestFlags.FLAG3.getValue());
        Value v6 = new Value(arena);
        v6.init(TestEnum.getType());
        v6.setEnum(TestEnum.VALUE2.getValue());

        map.put("integer", v1);
        map.put("boolean", v2);
        map.put("string", v3);
        map.put("strings", v4);
        map.put("flags", v5);
        map.put("enum", v6);

        return map;
    }

    private void compareGvalueMaps(Map<String, Value> map1, Map<String, Value> map2) {
        assertNotNull(map2);
        assertEquals(map1.keySet(), map2.keySet());
        for (String key : map1.keySet()) {
            Object v1 = ValueUtil.valueToObject(map1.get(key));
            Object v2 = ValueUtil.valueToObject(map2.get(key));
            if (v1 instanceof String[] s1 && v2 instanceof String[] s2)
                assertArrayEquals(s1, s2);
            else
                assertEquals(v1, v2);
        }
    }

    @Test
    void gvalueReturn() {
        try (Arena arena = Arena.ofConfined()) {
            compareGvalueMaps(gvalueMap(arena), testGhashGvalueReturn());
        }
    }

    @Test
    void gvalueIn() {
        try (Arena arena = Arena.ofConfined()) {
            testGhashGvalueIn(gvalueMap(arena));
        }
    }

    @Test
    void containerReturn() {
        assertEquals(EXPECTED_HASH, testGhashContainerReturn());
    }

    @Test
    void fullReturn() {
        assertEquals(EXPECTED_HASH, testGhashEverythingReturn());
    }

    @Test
    void nullIn() {
        testGhashNullIn(null);
    }

    @Test
    void nullOut() {
        var out = new Out<HashTable<String, String>>();
        testGhashNullOut(out);
        assertNotNull(out.get());
        assertTrue(out.get().isEmpty());
    }

    @Test
    void noneIn() {
        HashTable<String, String> map = new HashTable<>(
                GLib::strHash,
                GLib::strEqual,
                Interop::getStringFrom,
                Interop::getStringFrom);
        map.putAll(EXPECTED_HASH);

        testGhashNothingIn(map);
        testGhashNothingIn2(map);
    }

    @Test
    void nested() {
        HashTable<String, HashTable<String, String>> returned = testGhashNestedEverythingReturn();
        assertNotNull(returned);
        assertEquals(1, returned.size());
        assertTrue(returned.contains("wibble"));
        HashTable<String, String> ghash = returned.get("wibble");
        assertEquals(3, ghash.size());
        // We can't do anything else with the value.
        // Nested hash tables are not supported in Java-GI.
    }
}
