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

package org.javagi.gimarshallingtests;

import org.gnome.glib.GLib;
import org.gnome.glib.HashTable;
import org.javagi.base.Out;
import org.javagi.interop.Interop;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestGHashTable {
    @Test
    void intNoneReturn() {
        Map<Integer, Integer> hashTable = ghashtableIntNoneReturn();
        assertNotNull(hashTable);
        assertEquals(4, hashTable.size());
        assertEquals(1, hashTable.get(-1));
        assertEquals(0, hashTable.get(0));
        assertEquals(-1, hashTable.get(1));
        assertEquals(-2, hashTable.get(2));
    }

    @Test
    void utf8NoneReturn() {
        Map<String, String> hashTable = ghashtableUtf8NoneReturn();
        assertNotNull(hashTable);
        assertEquals(4, hashTable.size());
        assertEquals("1", hashTable.get("-1"));
        assertEquals("0", hashTable.get("0"));
        assertEquals("-1", hashTable.get("1"));
        assertEquals("-2", hashTable.get("2"));
    }

    @Test
    void utf8ContainerReturn() {
        Map<String, String> hashTable = ghashtableUtf8ContainerReturn();
        assertNotNull(hashTable);
        assertEquals(4, hashTable.size());
        assertEquals("1", hashTable.get("-1"));
        assertEquals("0", hashTable.get("0"));
        assertEquals("-1", hashTable.get("1"));
        assertEquals("-2", hashTable.get("2"));
    }

    @Test
    void utf8FullReturn() {
        Map<String, String> hashTable = ghashtableUtf8FullReturn();
        assertNotNull(hashTable);
        assertEquals(4, hashTable.size());
        assertEquals("1", hashTable.get("-1"));
        assertEquals("0", hashTable.get("0"));
        assertEquals("-1", hashTable.get("1"));
        assertEquals("-2", hashTable.get("2"));
    }

    @Test
    void intNoneIn() {
        HashTable<Integer, Integer> hashTable = new HashTable<>(null, null, pointer -> (int) pointer.address(), pointer -> (int) pointer.address());
        hashTable.put(-1, 1);
        hashTable.put(0, 0);
        hashTable.put(1, -1);
        hashTable.put(2, -2);
        ghashtableIntNoneIn(hashTable);
    }

    @Test
    void utf8NoneIn() {
        HashTable<String, String> hashTable = new HashTable<>(GLib::strHash, GLib::strEqual, Interop::getStringFrom, Interop::getStringFrom);
        hashTable.put("-1", "1");
        hashTable.put("0", "0");
        hashTable.put("1", "-1");
        hashTable.put("2", "-2");
        ghashtableUtf8NoneIn(hashTable);
    }

    @Test
    void utf8ContainerIn() {
        HashTable<String, String> hashTable = new HashTable<>(GLib::strHash, GLib::strEqual, Interop::getStringFrom, Interop::getStringFrom);
        hashTable.put("-1", "1");
        hashTable.put("0", "0");
        hashTable.put("1", "-1");
        hashTable.put("2", "-2");
        ghashtableUtf8ContainerIn(hashTable);
    }

    @Test
    void utf8FullIn() {
        HashTable<String, String> hashTable = new HashTable<>(GLib::strHash, GLib::strEqual, Interop::getStringFrom, Interop::getStringFrom);
        hashTable.put("-1", "1");
        hashTable.put("0", "0");
        hashTable.put("1", "-1");
        hashTable.put("2", "-2");
        ghashtableUtf8FullIn(hashTable);
    }

    @Test
    void doubleIn() {
        HashTable<String, Double> hashTable = new HashTable<>(GLib::strHash, GLib::strEqual, Interop::getStringFrom, Interop::getDoubleFrom);
        hashTable.put("-1", -0.1);
        hashTable.put("0", 0.0);
        hashTable.put("1", 0.1);
        hashTable.put("2", 0.2);
        ghashtableDoubleIn(hashTable);
    }

    @Test
    void floatIn() {
        HashTable<String, Float> hashTable = new HashTable<>(GLib::strHash, GLib::strEqual, Interop::getStringFrom, Interop::getFloatFrom);
        hashTable.put("-1", -0.1f);
        hashTable.put("0", 0.0f);
        hashTable.put("1", 0.1f);
        hashTable.put("2", 0.2f);
        ghashtableFloatIn(hashTable);
    }

    @Test
    void int64In() {
        HashTable<String, Long> hashTable = new HashTable<>(GLib::strHash, GLib::strEqual, Interop::getStringFrom, Interop::getLongFrom);
        hashTable.put("-1", -1L);
        hashTable.put("0", 0L);
        hashTable.put("1", 1L);
        hashTable.put("2", 0x100000000L);
        ghashtableInt64In(hashTable);
    }

    @Test
    void uint64In() {
        HashTable<String, Long> hashTable = new HashTable<>(GLib::strHash, GLib::strEqual, Interop::getStringFrom, Interop::getLongFrom);
        hashTable.put("-1", 0x100000000L);
        hashTable.put("0", 0L);
        hashTable.put("1", 1L);
        hashTable.put("2", 2L);
        ghashtableUint64In(hashTable);
    }

    @Test
    void utf8NoneOut() {
        var v = new Out<HashTable<String, String>>();
        ghashtableUtf8NoneOut(v);
        HashTable<String, String> hashTable = v.get();
        assertNotNull(hashTable);
        assertEquals(4, hashTable.size());
        assertEquals("1", hashTable.get("-1"));
        assertEquals("0", hashTable.get("0"));
        assertEquals("-1", hashTable.get("1"));
        assertEquals("-2", hashTable.get("2"));
    }

    @Test
    void utf8NoneOutUninitialized() {
        var v = new Out<HashTable<String, String>>();
        assertFalse(ghashtableUtf8NoneOutUninitialized(v));
    }

    @Test
    void utf8ContainerOut() {
        var v = new Out<HashTable<String, String>>();
        ghashtableUtf8ContainerOut(v);
        HashTable<String, String> hashTable = v.get();
        assertNotNull(hashTable);
        assertEquals(4, hashTable.size());
        assertEquals("1", hashTable.get("-1"));
        assertEquals("0", hashTable.get("0"));
        assertEquals("-1", hashTable.get("1"));
        assertEquals("-2", hashTable.get("2"));
    }

    @Test
    void utf8ContainerOutUninitialized() {
        var v = new Out<HashTable<String, String>>();
        assertFalse(ghashtableUtf8ContainerOutUninitialized(v));
    }

    @Test
    void utf8FullOut() {
        var v = new Out<HashTable<String, String>>();
        ghashtableUtf8FullOut(v);
        HashTable<String, String> hashTable = v.get();
        assertNotNull(hashTable);
        assertEquals(4, hashTable.size());
        assertEquals("1", hashTable.get("-1"));
        assertEquals("0", hashTable.get("0"));
        assertEquals("-1", hashTable.get("1"));
        assertEquals("-2", hashTable.get("2"));
    }

    @Test
    void utf8FullOutUninitialized() {
        var v = new Out<HashTable<String, String>>();
        assertFalse(ghashtableUtf8FullOutUninitialized(v));
    }

    @Test
    void utf8NoneInout() {
        HashTable<String, String> hashTable = new HashTable<>(GLib::strHash, GLib::strEqual, Interop::getStringFrom, Interop::getStringFrom);
        hashTable.put("-1", "1");
        hashTable.put("0", "0");
        hashTable.put("1", "-1");
        hashTable.put("2", "-2");
        var v = new Out<>(hashTable);
        ghashtableUtf8NoneInout(v);
        hashTable = v.get();
        assertEquals(3, hashTable.size());
        assertEquals("1", hashTable.get("-1"));
        assertEquals("0", hashTable.get("0"));
        assertEquals("1", hashTable.get("1"));
    }

    @Test
    void utf8ContainerInout() {
        HashTable<String, String> hashTable = new HashTable<>(GLib::strHash, GLib::strEqual, Interop::getStringFrom, Interop::getStringFrom);
        hashTable.put("-1", "1");
        hashTable.put("0", "0");
        hashTable.put("1", "-1");
        hashTable.put("2", "-2");
        var v = new Out<>(hashTable);
        ghashtableUtf8ContainerInout(v);
        hashTable = v.get();
        assertEquals(3, hashTable.size());
        assertEquals("1", hashTable.get("-1"));
        assertEquals("0", hashTable.get("0"));
        assertEquals("1", hashTable.get("1"));
    }

    @Test
    void utf8FullInout() {
        HashTable<String, String> hashTable = new HashTable<>(GLib::strHash, GLib::strEqual, Interop::getStringFrom, Interop::getStringFrom);
        hashTable.put("-1", "1");
        hashTable.put("0", "0");
        hashTable.put("1", "-1");
        hashTable.put("2", "-2");
        var v = new Out<>(hashTable);
        ghashtableUtf8FullInout(v);
        hashTable = v.get();
        assertEquals(3, hashTable.size());
        assertEquals("1", hashTable.get("-1"));
        assertEquals("0", hashTable.get("0"));
        assertEquals("1", hashTable.get("1"));
    }
}
