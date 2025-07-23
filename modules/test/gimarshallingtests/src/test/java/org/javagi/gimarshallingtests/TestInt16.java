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

public class TestInt16 {
    @Test
    void testInt16ReturnMax() {
        assertEquals(Short.MAX_VALUE, int16ReturnMax());
    }

    @Test
    void testInt16ReturnMin() {
        assertEquals(Short.MIN_VALUE, int16ReturnMin());
    }

    @Test
    void testInt16InMax() {
        int16InMax(Short.MAX_VALUE);
    }

    @Test
    void testInt16InMin() {
        int16InMin(Short.MIN_VALUE);
    }

    @Test
    void testInt16OutMax() {
        var v = new Out<>((short) 0);
        int16OutMax(v);
        assertEquals(Short.MAX_VALUE, v.get());
    }

    @Test
    void testInt16OutMin() {
        var v = new Out<>((short) 0);
        int16OutMin(v);
        assertEquals(Short.MIN_VALUE, v.get());
    }

    @Test
    void testInt16OutUninitialized() {
        var v = new Out<>((short) 0);
        assertFalse(int16OutUninitialized(v));
        assertEquals((short) 0, v.get());
    }

    @Test
    void testInt16OutMaxMin() {
        var v = new Out<>(Short.MAX_VALUE);
        int16InoutMaxMin(v);
        assertEquals(Short.MIN_VALUE, v.get());
    }

    @Test
    void testInt16OutMinMax() {
        var v = new Out<>(Short.MIN_VALUE);
        int16InoutMinMax(v);
        assertEquals(Short.MAX_VALUE, v.get());
    }

    @Test
    void testUint16Return() {
        assertEquals((short) -1, uint16Return());
    }

    @Test
    void testUint16In() {
        uint16In((short) -1);
    }

    @Test
    void testUint16Out() {
        var v = new Out<>((short) 0);
        uint16Out(v);
        assertEquals((short) -1, v.get());
    }

    @Test
    void testUint16OutUninitialized() {
        var v = new Out<>((short) 0);
        assertFalse(uint16OutUninitialized(v));
        assertEquals((short) 0, v.get());
    }

    @Test
    void testUint16Inout() {
        var v = new Out<>((short) -1);
        uint16Inout(v);
        assertEquals((short) 0, v.get());
    }
}
