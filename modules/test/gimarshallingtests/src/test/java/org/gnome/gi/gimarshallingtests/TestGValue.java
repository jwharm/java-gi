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

package org.gnome.gi.gimarshallingtests;

import org.gnome.gobject.Value;
import org.javagi.base.Out;
import org.javagi.gobject.types.Types;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestGValue {
    @Test
    void return_() {
        Value v = gvalueReturn();
        assertNotNull(v);
        assertEquals(42, v.getInt());
    }

    @Test
    void noncanonicalNanFloat() {
        Value v = gvalueNoncanonicalNanFloat();
        assertNotNull(v);
        assertEquals(Float.NaN, v.getFloat());
    }

    @Test
    void noncanonicalNanDouble() {
        Value v = gvalueNoncanonicalNanDouble();
        assertNotNull(v);
        assertEquals(Double.NaN, v.getDouble());
    }

    @Test
    void in() {
        Value v = new Value();
        v.init(Types.INT);
        v.setInt(42);
        gvalueIn(v);
    }

    @Test
    void int64In() {
        Value v = new Value();
        v.init(Types.INT64);
        v.setInt64(Long.MAX_VALUE);
        gvalueInt64In(v);
    }

    @Test
    void inWithType() {
        Value i = new Value();
        i.init(Types.INT);
        i.setInt(42);
        gvalueInWithType(i, Types.INT);

        Value d = new Value();
        d.init(Types.DOUBLE);
        d.setDouble(42.5d);
        gvalueInWithType(d, Types.DOUBLE);
    }

    @Test
    void inWithModification() {
        Value v = new Value();
        v.init(Types.INT);
        v.setInt(42);
        gvalueInWithModification(v);
        assertEquals(24, v.getInt());
    }

    @Test
    void inEnum() {
        Value v = new Value();
        v.init(Types.ENUM);
        v.setEnum(Enum.VALUE3.getValue());
        gvalueInEnum(v);
    }

    @Test
    void inFlags() {
        Value v = new Value();
        v.init(Types.FLAGS);
        v.setFlags(Flags.VALUE3.getValue());
        gvalueInFlags(v);
    }

    @Test
    void out() {
        var out = new Out<Value>();
        gvalueOut(out);
        assertEquals(42, out.get().getInt());
    }

    @Test
    void outUninitialized() {
        var out = new Out<Value>();
        assertFalse(gvalueOutUninitialized(out));
        assertNull(out.get());
    }

    @Test
    void int64Out() {
        var out = new Out<Value>();
        gvalueInt64Out(out);
        assertEquals(Long.MAX_VALUE, out.get().getInt64());
    }

    @Test
    void outCallerAllocates() {
        Value v = new Value();
        gvalueOutCallerAllocates(v);
        assertEquals(42, v.getInt());
    }

    @Test
    void inOut() {
        Value v = new Value();
        v.init(Types.INT);
        v.setInt(42);
        var out = new Out<>(v);
        gvalueInout(out);
        assertEquals("42", v.getString());
    }

    @Test
    void flatArray() {
        Value i = new Value();
        i.init(Types.INT);
        i.setInt(42);

        Value s = new Value();
        s.init(Types.STRING);
        s.setString("42");

        Value b = new Value();
        b.init(Types.BOOLEAN);
        b.setBoolean(true);

        Value[] values = new Value[] {i, s, b};
        gvalueFlatArray(values);
    }

    @Test
    void returnFlatArray() {
        Value[] values = returnGvalueFlatArray();
        assertNotNull(values);
        assertEquals(3, values.length);
        assertEquals(42, values[0].getInt());
        assertEquals("42", values[1].getString());
        assertTrue(values[2].getBoolean());
    }

    @Test
    void returnZeroTerminatedArray() {
        Value[] values = returnGvalueZeroTerminatedArray();
        assertNotNull(values);
        assertEquals(3, values.length);
        assertEquals(42, values[0].getInt());
        assertEquals("42", values[1].getString());
        assertTrue(values[2].getBoolean());
    }

    @Test
    void roundTrip() {
        Value a = new Value();
        a.init(Types.INT);
        a.setInt(42);
        Value b = gvalueRoundTrip(a);
        assertNotNull(b);
        assertEquals(a.readGType(), b.readGType());
        assertEquals(a.getInt(), b.getInt());
    }

    @Test
    void copy() {
        Value a = new Value();
        a.init(Types.INT);
        a.setInt(42);
        Value b = gvalueCopy(a);
        assertNotNull(b);
        assertNotEquals(a, b);
        assertEquals(a.readGType(), b.readGType());
        assertEquals(a.getInt(), b.getInt());
    }

    @Test
    void arrayRoundTrip() {
        Value i = new Value();
        i.init(Types.INT);
        i.setInt(42);

        Value s = new Value();
        s.init(Types.STRING);
        s.setString("42");

        Value b = new Value();
        b.init(Types.BOOLEAN);
        b.setBoolean(true);

        Value[] values = gvalueFlatArrayRoundTrip(i, s, b);

        assertNotNull(values);
        assertEquals(3, values.length);

        assertNotNull(values[0]);
        assertNotNull(values[1]);
        assertNotNull(values[2]);

        assertEquals(42, values[0].getInt());
        assertEquals("42", values[1].getString());
        assertTrue(values[2].getBoolean());
    }

    @Test
    void float_() {
        Value f = new Value();
        f.init(Types.FLOAT);
        f.setFloat(3.14f);

        Value d = new Value();
        d.init(Types.DOUBLE);
        d.setDouble(3.14d);

        gvalueFloat(f, d);
    }
}
