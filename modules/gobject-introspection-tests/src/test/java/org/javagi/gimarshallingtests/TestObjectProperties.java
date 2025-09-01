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

import org.gnome.gi.gimarshallingtests.*;
import org.gnome.glib.List;
import org.gnome.glib.Variant;
import org.gnome.gobject.GObject;
import org.gnome.gobject.Value;
import org.javagi.gobject.types.Types;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestObjectProperties {
    PropertiesObject obj;

    @BeforeEach
    void constructObject() {
        obj = new PropertiesObject();
    }

    @Test
    void getSetBoolean() {
        obj.setProperty("some-boolean", true);
        assertEquals(true, obj.getProperty("some-boolean"));
    }

    @Test
    void getSetChar() {
        obj.setProperty("some-char", 'a');
        assertEquals('a', obj.getProperty("some-char"));
    }

    @Test
    void getSetUChar() {
        obj.setProperty("some-uchar", 'a');
        assertEquals('a', obj.getProperty("some-uchar"));
    }

    @Test
    void getSetInt() {
        obj.setProperty("some-int", 42);
        assertEquals(42, obj.getProperty("some-int"));
    }

    @Test
    void getSetUInt() {
        obj.setProperty("some-uint", 42);
        assertEquals(42, obj.getProperty("some-uint"));
    }

    @Test
    void getSetLong() {
        obj.setProperty("some-long", 42);
        assertEquals(42, obj.getProperty("some-long"));
    }

    @Test
    void getSetULong() {
        obj.setProperty("some-ulong", 42);
        assertEquals(42, obj.getProperty("some-ulong"));
    }

    @Test
    void getSetInt64() {
        obj.setProperty("some-int64", 42L);
        assertEquals(42L, obj.getProperty("some-int64"));
    }

    @Test
    void getSetString() {
        obj.setProperty("some-string", "test");
        assertEquals("test", obj.getProperty("some-string"));
        obj.setProperty("some-string", (String) null);
        assertNull(obj.getProperty("some-string"));
    }

    @Test
    void getSetFloat() {
        AtomicInteger count = new AtomicInteger(0);
        obj.onNotify("some-float", _ -> count.incrementAndGet());
        obj.setProperty("some-float", 3.14f);
        assertEquals(1, count.get());
        assertEquals(3.14f, obj.getProperty("some-float"));
    }

    @Test
    void getSetDouble() {
        AtomicInteger count = new AtomicInteger(0);
        obj.onNotify("some-double", _ -> count.incrementAndGet());
        obj.setProperty("some-double", Math.E);
        assertEquals(1, count.get());
        assertEquals(Math.E, obj.getProperty("some-double"));
    }

    @Test
    void getSetStrv() {
        var array = new String[] {"0", "1", "2"};
        obj.setProperty("some-strv", array);
        assertArrayEquals(array, (String[]) obj.getProperty("some-strv"));
    }

    @Test
    void getSetBoxedStruct() {
        var in = new BoxedStruct();
        in.writeLong(6);
        obj.setProperty("some-boxed-struct", in);
        var out = (BoxedStruct) obj.getProperty("some-boxed-struct");
        assertEquals(in.readLong(), out.readLong());
    }

    @Test
    void getSetBoxedGList() {
        List<Integer> glist = glistIntNoneReturn();
        obj.setProperty("some-boxed-glist", glist);
        // Currently unsupported: The boxed type is not registered, and
        // the type hint in the doc comment is not usable here
        assertThrows(UnsupportedOperationException.class,
                     () -> obj.getProperty("some-boxed-glist"));
    }

    @Test
    void getSetGValue() {
        var in = new Value();
        in.init(Types.INT);
        in.setInt(42);
        // cast to Object to ensure we use the correct overload
        obj.setProperty("some-gvalue", (Object) in);
        var out = (Value) obj.getProperty("some-gvalue");
        assertEquals(in.getInt(), out.getInt());
    }

    @Test
    void getSetGVariant() {
        var in = new Variant("i", 42);
        obj.setProperty("some-variant", in);
        var out = (Variant) obj.getProperty("some-variant");
        assertEquals(in.getInt32(), out.getInt32());
    }

    @Test
    void getSetGObject() {
        var in = new GObject();
        obj.setProperty("some-object", in);
        var out = (GObject) obj.getProperty("some-object");
        assertEquals(in, out);

        in = new GIMarshallingTestsObject(42);
        obj.setProperty("some-object", in);
        out = (GIMarshallingTestsObject) obj.getProperty("some-object");
        assertEquals(42, out.getProperty("int"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getSetFlags() {
        // Set a single flag
        Flags single = Flags.VALUE2;
        obj.setProperty("some-flags", single);
        Set<Flags> out = (Set<Flags>) obj.getProperty("some-flags");
        assertEquals(Set.of(single), out);

        // Combine multiple flags
        Set<Flags> multiple = Set.of(Flags.VALUE1, Flags.VALUE2);
        obj.setProperty("some-flags", multiple);
        out = (Set<Flags>) obj.getProperty("some-flags");
        assertEquals(multiple, out);
    }

    @Test
    void getSetEnum() {
        obj.setProperty("some-enum", GEnum.VALUE2);
        assertEquals(GEnum.VALUE2, obj.getProperty("some-enum"));
    }

    @Test
    void getSetByteArray() {
        byte[] in = new byte[] {1, 2, 3};
        obj.setProperty("some-byte-array", in);
        byte[] out = (byte[]) obj.getProperty("some-byte-array");
        assertArrayEquals(in, out);
    }

    @Test
    void readOnlyProperty() {
        assertEquals(42, obj.getProperty("some-readonly"));

        // Java-GI doesn't check for read-only properties, so the following
        // line will not throw, but GLib will log a CRITICAL error:
        // obj.setProperty("some-readonly", 42);
    }

    @Test
    void deprecatedProperty() {
        obj.setProperty("some-deprecated-int", 42);
        assertEquals(42, obj.getProperty("some-deprecated-int"));
    }
}
