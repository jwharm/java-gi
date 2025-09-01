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

import org.gnome.glib.Variant;
import org.gnome.gobject.GObject;
import org.gnome.gobject.Value;
import org.javagi.gobject.JavaClosure;
import org.javagi.gobject.types.Types;
import org.junit.jupiter.api.Test;

import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestSimpleMarshalling {
    @Test
    void returnAllowNone() {
        assertNull(testReturnAllowNone());
    }

    @Test
    void returnNullable() {
        assertNull(testReturnNullable());
    }

    @Test
    void boolean_() {
        assertFalse(testBoolean(false));
        assertTrue(testBoolean(true));
        assertTrue(testBooleanTrue(true));
        assertFalse(testBooleanFalse(false));
    }

    @Test
    void int8() {
        assertEquals(42, testInt8((byte) 42));
        assertEquals(-42, testInt8((byte) -42));
        assertEquals(42, testUint8((byte) 42));
    }

    @Test
    void int16() {
        assertEquals(42, testInt16((short) 42));
        assertEquals(-42, testInt16((short) -42));
        assertEquals(42, testUint16((short) 42));
    }

    @Test
    void int32() {
        assertEquals(42, testInt32(42));
        assertEquals(-42, testInt32(-42));
        assertEquals(42, testUint32(42));
    }

    @Test
    void int64() {
        assertEquals(42, testInt64(42));
        assertEquals(-42, testInt64(-42));
        assertEquals(42, testUint64(42));
    }

    @Test
    void short_() {
        assertEquals(42, testShort((short) 42));
        assertEquals(-42, testShort((short) -42));
        assertEquals(42, testUshort((short) 42));
    }

    @Test
    void int_() {
        assertEquals(42, testInt(42));
        assertEquals(-42, testInt(-42));
        assertEquals(42, testUint(42));
    }

    @Test
    void long_() {
        assertEquals(42, testLong(42));
        assertEquals(-42, testLong(-42));
        assertEquals(42, testLong(42));
    }

    @Test
    void size() {
        assertEquals(42, testSsize(42));
        assertEquals(-42, testSsize(-42));
        assertEquals(42, testSize(42));
        assertEquals(-42, testSize(-42));
    }

    @Test
    void float_() {
        assertEquals(42.42f, testFloat(42.42f));
        assertEquals(-42.42f, testFloat(-42.42f));
        assertEquals(42.42f, testFloat(42.42f));
        assertEquals(Float.NaN, testFloat(Float.NaN));
    }

    @Test
    void double_() {
        assertEquals(42.42, testDouble(42.42));
        assertEquals(-42.42, testDouble(-42.42));
        assertEquals(42.42, testDouble(42.42));
        assertEquals(Double.NaN, testDouble(Double.NaN));
    }

    @Test
    void unichar() {
        assertEquals('c', testUnichar('c'));
        assertEquals('\0', testUnichar('\0'));
        assertEquals('♥', testUnichar('♥'));
    }

    @Test
    void time_t() {
        var now = System.currentTimeMillis();
        assertEquals(now, testTimet(now));
    }

    @Test
    void off_t() {
        assertEquals(0x7fff_ffff, testOfft(0x7fff_ffff));
    }

    @Test
    void gtype() {
        assertEquals(Types.NONE, testGtype(Types.NONE));
        assertEquals(Types.STRING, testGtype(Types.STRING));
        assertEquals(GObject.getType(), testGtype(GObject.getType()));
    }

    @Test
    void closure() {
        var closure = new JavaClosure((IntSupplier) () -> 42);
        assertEquals(42, testClosure(closure));
    }

    @Test
    void closureWithOneArg() {
        var closure = new JavaClosure((IntUnaryOperator) a -> a * 2);
        assertEquals(42, testClosureOneArg(closure, 21));
    }

    @Test
    void closureVariant() {
        var closure = new JavaClosure((UnaryOperator<Variant>) v -> v);
        var variant = new Variant("i", 42);
        var result = testClosureVariant(closure, variant);
        assertNotNull(result);
        assertEquals(42, result.getInt32());
    }

    @Test
    void intValueArg() {
        var value = new Value();
        value.init(Types.INT);
        value.setInt(42);
        assertEquals(42, testIntValueArg(value));
    }

    @Test
    void valueReturn() {
        var value = testValueReturn(42);
        assertNotNull(value);
        assertEquals(42, value.getInt());
    }
}
