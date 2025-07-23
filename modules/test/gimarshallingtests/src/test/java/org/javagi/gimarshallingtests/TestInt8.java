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

import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestInt8 {
    @Test
    void testInt8ReturnMax() {
        assertEquals(Byte.MAX_VALUE, int8ReturnMax());
    }

    @Test
    void testInt8ReturnMin() {
        assertEquals(Byte.MIN_VALUE, int8ReturnMin());
    }

    @Test
    void testInt8InMax() {
        int8InMax(Byte.MAX_VALUE);
    }

    @Test
    void testInt8InMin() {
        int8InMin(Byte.MIN_VALUE);
    }

    @Test
    void testInt8OutMax() {
        var v = new Out<>((byte) 0);
        int8OutMax(v);
        assertEquals(Byte.MAX_VALUE, v.get());
    }

    @Test
    void testInt8OutMin() {
        var v = new Out<>((byte) 0);
        int8OutMin(v);
        assertEquals(Byte.MIN_VALUE, v.get());
    }

    @Test
    void testInt8OutUninitialized() {
        var v = new Out<>((byte) 0);
        assertFalse(int8OutUninitialized(v));
        assertEquals((byte) 0, v.get());
    }

    @Test
    void testInt8OutMaxMin() {
        var v = new Out<>(Byte.MAX_VALUE);
        int8InoutMaxMin(v);
        assertEquals(Byte.MIN_VALUE, v.get());
    }

    @Test
    void testInt8OutMinMax() {
        var v = new Out<>(Byte.MIN_VALUE);
        int8InoutMinMax(v);
        assertEquals(Byte.MAX_VALUE, v.get());
    }

    /*
     * Note: We can't express unsigned int8 in Java.
     * Casting G_MAXUINT8 to a signed byte == -1.
     */

    @Test
    void testUint8Return() {
        assertEquals((byte) -1, uint8Return());
    }

    @Test
    void testUint8In() {
        uint8In((byte) -1);
    }

    @Test
    void testUint8Out() {
        var v = new Out<>((byte) 0);
        uint8Out(v);
        assertEquals((byte) -1, v.get());
    }

    @Test
    void testUint8OutUninitialized() {
        var v = new Out<>((byte) 0);
        assertFalse(uint8OutUninitialized(v));
        assertEquals((byte) 0, v.get());
    }

    @Test
    void testUint8Inout() {
        var v = new Out<>((byte) -1);
        uint8Inout(v);
        assertEquals((byte) 0, v.get());
    }
}
