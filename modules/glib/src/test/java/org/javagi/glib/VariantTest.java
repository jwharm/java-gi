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

package org.javagi.glib;

import org.gnome.glib.Variant;
import org.gnome.glib.VariantType;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class VariantTest {
    @Test
    void null_() {
        assertNull(Variant.pack(null).unpack());
    }

    @Test
    void boolean_() {
        assertEquals(true, Variant.pack(true).unpack());
        assertEquals(false, Variant.pack(false).unpack());
    }

    @Test
    void byte_() {
        assertEquals((byte) 1, Variant.pack((byte) 1).unpack());
        assertEquals((byte) 0, Variant.pack((byte) 0).unpack());
    }

    @Test
    void char_() {
        // char is packed as a single-character string
        assertEquals("j", Variant.pack('j').unpack());
    }

    // Compare two double/float values with 5 digits of equality
    private static void assertIsClose(double d1, double d2) {
        assertTrue(Math.abs(d1-d2) < 0.000001d);
    }

    @Test
    void double_() {
        assertIsClose(42.123, (double) Variant.pack(42.123).unpack());
        assertEquals(Double.MAX_VALUE, Variant.pack(Double.MAX_VALUE).unpack());
    }

    @Test
    void float_() {
        assertIsClose(42.456f, (double) Variant.pack(42.456f).unpack());
        assertEquals(Double.NaN, Variant.pack(Float.NaN).unpack());
    }

    @Test
    void int_() {
        assertEquals(42, Variant.pack(42).unpack());
        assertEquals(-42, Variant.pack(-42).unpack());
        assertEquals(Integer.MIN_VALUE, Variant.pack(Integer.MIN_VALUE).unpack());
        assertEquals(Integer.MAX_VALUE, Variant.pack(Integer.MAX_VALUE).unpack());
    }

    @Test
    void long_() {
        assertEquals(42L, Variant.pack(42L).unpack());
        assertEquals(Long.MIN_VALUE, Variant.pack(Long.MIN_VALUE).unpack());
        assertEquals(Long.MAX_VALUE, Variant.pack(Long.MAX_VALUE).unpack());
    }

    @Test
    void short_() {
        assertEquals((short) 42, Variant.pack((short) 42).unpack());
    }

    @Test
    void string() {
        assertEquals("abc", Variant.pack("abc").unpack());
        // String containing \0 is not supported
        assertEquals("abc", Variant.pack("abc\0def").unpack());
    }

    @Test
    void list() {
        var list1 = List.of(1, 2, 3);
        assertEquals(list1, Variant.pack(list1).unpack());

        var list2 = List.of("a", "b", "c");
        assertEquals(list2, Variant.pack(list2).unpack());

        var list3 = List.of(list1, list1);
        assertEquals(list3, Variant.pack(list3).unpack());
    }

    @Test
    void set() {
        var set = Set.of(1, 2, 3);
        // Set is packed as an array variant, and returned as a list.
        // Copy the unpacked list back into a set.
        assertEquals(set, Set.copyOf((Collection<?>) Variant.pack(set).unpack()));
    }

    @Test
    void map() {
        var map1 = Map.of(1, "str1", 2, "str2", 3, "str3");
        assertEquals(map1, Variant.pack(map1).unpack());

        var map2 = Map.of("a", "str1", "b", "str2", "c", "str3");
        assertEquals(map2, Variant.pack(map2).unpack());

        var map3 = Map.of(42.1, map1, 42.2, map1);
        assertEquals(map3, Variant.pack(map3).unpack());
    }

    @Test
    void optional() {
        var opt1 = Optional.of("str");
        var variant1 = Variant.pack(opt1);
        assertTrue(variant1.getVariantType().isMaybe());
        assertEquals("str", variant1.unpack());

        var opt2 = Optional.empty();
        var variant2 = Variant.pack(opt2);
        assertTrue(variant2.getVariantType().isMaybe());
        assertNull(variant2.unpack());
    }

    @Test
    void recursive() {
        Variant str = Variant.pack("str");
        Variant list = Variant.pack(List.of(str));
        assertEquals("av", list.getTypeString());

        // recursive
        Object o1 = list.unpackRecursive();
        assertInstanceOf(List.class, o1);
        List<?> list1 = (List<?>) o1;
        assertInstanceOf(String.class, list1.getFirst());

        // not recursive
        Object o2 = list.unpack();
        assertInstanceOf(List.class, o2);
        List<?> list2 = (List<?>) o2;
        assertInstanceOf(Variant.class, list2.getFirst());
    }

    @Test
    void tuple() {
        Variant tuple = new Variant("(ins)", 42, (short) 43, "44");
        Object unpacked = tuple.unpack();
        ArrayList<Object> expected = new ArrayList<>();
        expected.add(42);
        expected.add((short) 43);
        expected.add("44");
        assertEquals(expected, unpacked);
    }

    @Test
    void toStringOverride() {
        assertEquals("'abc'", Variant.string("abc").toString());
        assertEquals("ms", new VariantType("ms").toString());
    }
}
