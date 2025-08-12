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

public class TestObjectPropertiesAccessors {
    PropertiesAccessorsObject obj;

    @BeforeEach
    void constructObject() {
        obj = new PropertiesAccessorsObject();
    }

    @Test
    void getSetBoolean() {
        obj.setBoolean(true);
        assertTrue(obj.getBoolean());
    }

    @Test
    void getSetChar() {
        obj.setChar((byte) 'a');
        assertEquals((byte) 'a', obj.getChar());
    }

    @Test
    void getSetUChar() {
        obj.setUchar((byte) 'a');
        assertEquals((byte) 'a', obj.getUchar());
    }

    @Test
    void getSetInt() {
        obj.setInt(42);
        assertEquals(42, obj.getInt());
    }

    @Test
    void getSetUInt() {
        obj.setUint(42);
        assertEquals(42, obj.getUint());
    }

    @Test
    void getSetLong() {
        obj.setLong(42);
        assertEquals(42, obj.getLong());
    }

    @Test
    void getSetULong() {
        obj.setUlong(42);
        assertEquals(42, obj.getUlong());
    }

    @Test
    void getSetInt64() {
        obj.setInt64(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, obj.getInt64());
    }

    @Test
    void getSetString() {
        obj.setString("test");
        assertEquals("test", obj.getString());

        obj.setString(null);
        assertNull(obj.getString());
    }

    @Test
    void getSetFloat() {
        AtomicInteger count = new AtomicInteger(0);
        obj.onNotify("some-float", _ -> count.incrementAndGet());
        obj.setFloat(Float.NaN);
        assertEquals(1, count.get());
        assertEquals(Float.NaN, obj.getFloat());
    }

    @Test
    void getSetDouble() {
        AtomicInteger count = new AtomicInteger(0);
        obj.onNotify("some-double", _ -> count.incrementAndGet());
        obj.setDouble(Math.E);
        assertEquals(1, count.get());
        assertEquals(Math.E, obj.getDouble());
    }

    @Test
    void getSetStrv() {
        var array = new String[] {"0", "1", "2"};
        obj.setStrv(array);
        assertArrayEquals(array,obj.getStrv());
    }

    @Test
    void getSetBoxedStruct() {
        var in = new BoxedStruct();
        in.writeLong(6);
        obj.setBoxedStruct(in);
        var out = obj.getBoxedStruct();
        assertNotNull(out);
        assertEquals(in.readLong(), out.readLong());
    }

    @Test
    void getSetBoxedGList() {
        List<Integer> in = glistIntNoneReturn();
        obj.setBoxedGlist(in);
        List<Integer> out = obj.getBoxedGlist();
        assertIterableEquals(in, out);
    }

    @Test
    void getSetGValue() {
        var in = new Value();
        in.init(Types.INT);
        in.setInt(42);
        obj.setGvalue(in);
        var out = obj.getGvalue();
        assertNotNull(out);
        assertEquals(in.getInt(), out.getInt());
    }

    @Test
    void getSetGVariant() {
        var in = new Variant("i", 42);
        obj.setVariant(in);
        var out = obj.getVariant();
        assertNotNull(out);
        assertEquals(in.getInt32(), out.getInt32());
    }

    @Test
    void getSetGObject() {
        var in = new GObject();
        obj.setObject(in);
        var out = obj.getObject();
        assertEquals(in, out);

        in = new GIMarshallingTestsObject(42);
        obj.setObject(in);
        out = obj.getObject();
        assertNotNull(out);
        assertEquals(42, out.getProperty("int"));
    }

    @Test
    void getSetFlags() {
        // Set a single flag
        Flags single = Flags.VALUE2;
        obj.setFlags(single);
        Set<Flags> out = obj.getFlags();
        assertEquals(Set.of(single), out);

        // Combine multiple flags
        Set<Flags> multiple = Set.of(Flags.VALUE1, Flags.VALUE2);
        obj.setFlags(multiple);
        out = obj.getFlags();
        assertEquals(multiple, out);

        // Combine multiple flags in varargs parameter
        obj.setFlags(Flags.VALUE2, Flags.VALUE1);
        out = obj.getFlags();
        assertEquals(multiple, out);
    }

    @Test
    void getSetEnum() {
        obj.setEnum(GEnum.VALUE2);
        assertEquals(GEnum.VALUE2, obj.getEnum());
    }

    @Test
    void getSetByteArray() {
        byte[] in = new byte[] {1, 2, 3};
        obj.setByteArray(in);
        byte[] out = obj.getByteArray();
        assertArrayEquals(in, out);
    }

    @Test
    void readOnlyProperty() {
        assertEquals(42, obj.getReadonly());
    }

    @Test
    void deprecatedProperty() {
        obj.setDeprecatedInt(42);
        assertEquals(42, obj.getDeprecatedInt());
    }
}
