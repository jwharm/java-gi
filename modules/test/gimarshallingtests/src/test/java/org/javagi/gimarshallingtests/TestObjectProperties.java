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

import org.gnome.gi.gimarshallingtests.BoxedStruct;
import org.gnome.gi.gimarshallingtests.PropertiesObject;
import org.gnome.glib.Strv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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

    @Test @Disabled // Boxed GValues not implemented yet
    void getSetStrv() {
        var array = new String[] {"0", "1", "2"};
        obj.setProperty("some-strv", array);
        assertArrayEquals(array, (String[]) obj.getProperty("some-strv"));
    }

    @Test @Disabled // Boxed GValues not implemented yet
    void getSetBoxedStruct() {
        var struct = new BoxedStruct();
        obj.setProperty("some-boxed-struct", struct);
        assertEquals(struct, obj.getProperty("some-boxed-struct"));
    }

    @Test @Disabled // Boxed GValues not implemented yet
    void getSetBoxedGList() {
        var glist = glistIntNoneReturn();
        obj.setProperty("some-boxed-glist", glist);
        assertEquals(glist, obj.getProperty("some-boxed-glist"));
    }
}
