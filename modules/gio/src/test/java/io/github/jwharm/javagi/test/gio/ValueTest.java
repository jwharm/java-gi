/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.test.gio;

import io.github.jwharm.javagi.gobject.types.Types;
import org.gnome.gio.SimpleAction;
import org.gnome.glib.*;
import org.gnome.gobject.GObject;
import org.gnome.gobject.Value;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test marshaling various parameter types to and from GValues.
 */
public class ValueTest {

    @Test
    public void booleanValue() {
        boolean b = true;
        Value boolValue = Value.allocate().init(Types.BOOLEAN);
        boolValue.setBoolean(b);
        assertEquals(boolValue.getBoolean(), b);
    }

    @Test
    public void doubleValue() {
        double d = 12345.6789;
        Value doubleValue = Value.allocate().init(Types.DOUBLE);
        doubleValue.setDouble(d);
        assertEquals(doubleValue.getDouble(), d);
    }

    @Test
    public void floatValue() {
        float f = 12.345f;
        Value floatValue = Value.allocate().init(Types.FLOAT);
        floatValue.setFloat(f);
        assertEquals(floatValue.getFloat(), f);
    }

    @Test
    public void intValue() {
        int i = 15;
        Value intValue = Value.allocate().init(Types.INT);
        intValue.setInt(i);
        assertEquals(intValue.getInt(), i);
    }

    @Test
    public void longValue() {
        // long values are marshaled to int, for Windows compatibility.
        int i = 15;
        Value longValue = Value.allocate().init(Types.LONG);
        longValue.setLong(i);
        assertEquals(longValue.getLong(), i);
    }

    @Test
    public void stringValue() {
        String s = "Test";
        Value strValue = Value.allocate().init(Types.STRING);
        strValue.setString(s);
        assertEquals(strValue.getString(), s);
    }

    @Test
    public void objectValue() {
        // value.getObject() == o, because getObject() will retrieve o from the InstanceCache
        GObject o = new SimpleAction("test", null);
        Value objValue = Value.allocate().init(Types.OBJECT);
        objValue.setObject(o);
        assertEquals(objValue.getObject(), o);
    }

    @Test
    public void boxedValue() {
        // compare a boxed value with its duplicate
        Date date = Date.newDmy(new DateDay((byte) 3), DateMonth.JUNE, new DateYear((short) 2023));
        Value boxedValue = Value.allocate().init(Date.getType());
        boxedValue.setBoxed(date.handle());
        Date dup = new Date(boxedValue.dupBoxed());
        assertEquals(date.compare(dup), 0);
    }
}
