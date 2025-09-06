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

import org.gnome.gi.regress.TestBoxed;
import org.gnome.gi.regress.TestObj;
import org.gnome.gi.regress.TestSubObj;
import org.gnome.glib.GLib;
import org.gnome.glib.HashTable;
import org.gnome.glib.List;
import org.gnome.gobject.GObject;
import org.gnome.gobject.InitiallyUnowned;
import org.javagi.base.Proxy;
import org.javagi.base.TransferOwnership;
import org.javagi.interop.Interop;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestObjectProperties {
    @Test
    void boxed() {
        var boxed1 = new TestBoxed();
        boxed1.writeSomeInt8((byte) 127);

        var object = TestObj.builder()
                .setBoxed(boxed1)
                .build();

        var boxed2 = (TestBoxed) object.getProperty("boxed");
        assertEquals((byte) 127, boxed2.readSomeInt8());

        var boxed3 = new TestBoxed();
        boxed3.writeSomeInt8((byte) 31);
        object.setProperty("boxed", boxed3);

        var boxed4 = (TestBoxed) object.getProperty("boxed");
        assertEquals((byte) 31, boxed4.readSomeInt8());
    }

    @Test
    void hashTable() {
        HashTable<String, Byte> hashTable1 = new HashTable<>(
                GLib::strHash,
                GLib::strEqual,
                Interop::getStringFrom,
                Interop::getByteFrom);
        hashTable1.putAll(Map.of("a", (byte) 1, "b", (byte) 2));

        var object = TestObj.builder()
                .setHashTable(hashTable1)
                .build();

        var hashTable2 = object.getProperty("hash-table");
        // We can't compare the hashtable contents, because Java-GI doesn't
        // know the key and value types of the hashtable that was returned
        // by GObject.getProperty().
        // Check whether the returned object is a hashtable and it is the
        // same as the original.
        assertInstanceOf(HashTable.class, hashTable2);
        assertEquals(hashTable1.handle(), ((Proxy) hashTable2).handle());
    }

    @Test
    void list() {
        var list1 = new List<>(Interop::getStringFrom, null, TransferOwnership.NONE);
        list1.add("a");

        var object = TestObj.builder()
                .setList(list1)
                .build();

        // Java-GI doesn't know that GObject.getProperty() returned a GList
        // in this case, so we end up with the raw memory address.
        var list2Address = (MemorySegment) object.getProperty("list");
        var list2 = new List<>(list2Address, Interop::getStringFrom, null, TransferOwnership.NONE);
        assertEquals("a", list2.getFirst());
    }

    @Test
    @Disabled("Property of GPtrArray of utf-8 elements is not supported")
    void pointerArray() {
        // TestObj.Builder.setPptrarray() initializes the GValue as a STRV type.
        // This is not correct, it should be a gpointer type.
        String[] array = new String[] {"a", "b", "c"};

        var object = TestObj.builder()
                .setPptrarray(array)
                .build();

        var result = (String[]) object.getProperty("pptrarray");
        assertArrayEquals(array, result);
    }

    @Test
    void hashTableOld() {
        HashTable<String, Byte> hashTable1 = new HashTable<>(
                GLib::strHash,
                GLib::strEqual,
                Interop::getStringFrom,
                Interop::getByteFrom);
        hashTable1.putAll(Map.of("a", (byte) 1, "b", (byte) 2));

        var object = TestObj.builder()
                .setHashTableOld(hashTable1)
                .build();

        var hashTable2 = object.getProperty("hash-table-old");
        assertInstanceOf(HashTable.class, hashTable2);
        assertEquals(hashTable1.handle(), ((Proxy) hashTable2).handle());
    }

    @Test
    void listOld() {
        var list1 = new List<>(Interop::getStringFrom, null, TransferOwnership.NONE);
        list1.add("a");

        var object = TestObj.builder()
                .setListOld(list1)
                .build();

        var list2Address = (MemorySegment) object.getProperty("list-old");
        var list2 = new List<>(list2Address, Interop::getStringFrom, null, TransferOwnership.NONE);
        assertEquals("a", list2.getFirst());
    }

    @Test
    void integer() {
        var object = TestObj.builder().setInt(42).build();
        assertEquals(42, object.getProperty("int"));
        object.setProperty("int", 43);
        assertEquals(43, object.getProperty("int"));
    }

    @Test
    void float_() {
        var object = TestObj.builder().setFloat(42.0f).build();
        assertEquals(42.0f, object.getProperty("float"));
        object.setProperty("float", 43.0f);
        assertEquals(43.0f, object.getProperty("float"));
    }

    @Test
    void double_() {
        var object = TestObj.builder().setDouble(42.0).build();
        assertEquals(42.0, object.getProperty("double"));
        object.setProperty("double", 43.0);
        assertEquals(43.0, object.getProperty("double"));
    }

    @Test
    void string() {
        var object = TestObj.builder().setString("42").build();
        assertEquals("42", object.getProperty("string"));
        object.setProperty("string", "43");
        assertEquals("43", object.getProperty("string"));
    }

    @Test
    void gtype() {
        var object = TestObj.builder().setGtype(GObject.getType()).build();
        assertEquals(GObject.getType(), object.getProperty("gtype"));
        object.setProperty("gtype", InitiallyUnowned.getType());
        assertEquals(InitiallyUnowned.getType(), object.getProperty("gtype"));
    }

    @Test
    void byteArray() {
        var object = TestObj.builder().setByteArray("abc".getBytes()).build();
        assertArrayEquals("abc".getBytes(), (byte[]) object.getProperty("byte-array"));
        object.setProperty("byte-array", "1234".getBytes());
        assertArrayEquals("1234".getBytes(), (byte[]) object.getProperty("byte-array"));
    }

    @Test
    void object() {
        var o1 = new GObject();
        var t1 = TestObj.builder().setBare(o1).build();
        var t2 = TestSubObj.builder().setBare(o1).build();

        assertEquals(o1, t1.getProperty("bare"));
        assertEquals(o1, t2.getProperty("bare"));

        var o2 = new GObject();
        t2.setProperty("bare", o2);
        assertEquals(o2, t2.getProperty("bare"));

        t2.setProperty("bare", (GObject) null); // try to set to null
        assertEquals(o2, t2.getProperty("bare")); // unchanged
    }
}
