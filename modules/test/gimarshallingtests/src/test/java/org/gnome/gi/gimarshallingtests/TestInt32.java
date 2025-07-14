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

import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestInt32 {
    @Test
    void testInt32ReturnMax() {
        assertEquals(Integer.MAX_VALUE, int32ReturnMax());
    }

    @Test
    void testInt32ReturnMin() {
        assertEquals(Integer.MIN_VALUE, int32ReturnMin());
    }

    @Test
    void testInt32InMax() {
        int32InMax(Integer.MAX_VALUE);
    }

    @Test
    void testInt32InMin() {
        int32InMin(Integer.MIN_VALUE);
    }

    @Test
    void testInt32OutMax() {
        var v = new Out<>(0);
        int32OutMax(v);
        assertEquals(Integer.MAX_VALUE, v.get());
    }

    @Test
    void testInt32OutMin() {
        var v = new Out<>(0);
        int32OutMin(v);
        assertEquals(Integer.MIN_VALUE, v.get());
    }

    @Test
    void testInt32OutUninitialized() {
        var v = new Out<>(0);
        assertFalse(int32OutUninitialized(v));
        assertEquals(0, v.get());
    }

    @Test
    void testInt32OutMaxMin() {
        var v = new Out<>(Integer.MAX_VALUE);
        int32InoutMaxMin(v);
        assertEquals(Integer.MIN_VALUE, v.get());
    }

    @Test
    void testInt32OutMinMax() {
        var v = new Out<>(Integer.MIN_VALUE);
        int32InoutMinMax(v);
        assertEquals(Integer.MAX_VALUE, v.get());
    }

    @Test
    void testUint32Return() {
        assertEquals(-1, uint32Return());
    }

    @Test
    void testUint32In() {
        uint32In(-1);
    }

    @Test
    void testUint32Out() {
        var v = new Out<>(0);
        uint32Out(v);
        assertEquals(-1, v.get());
    }

    @Test
    void testUint32OutUninitialized() {
        var v = new Out<>(0);
        assertFalse(uint32OutUninitialized(v));
        assertEquals(0, v.get());
    }

    @Test
    void testUint32Inout() {
        var v = new Out<>(-1);
        uint32Inout(v);
        assertEquals(0, v.get());
    }
}
