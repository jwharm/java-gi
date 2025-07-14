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

public class TestInt64 {
    @Test
    void testInt64ReturnMax() {
        assertEquals(Long.MAX_VALUE, int64ReturnMax());
    }

    @Test
    void testInt64ReturnMin() {
        assertEquals(Long.MIN_VALUE, int64ReturnMin());
    }

    @Test
    void testInt64InMax() {
        int64InMax(Long.MAX_VALUE);
    }

    @Test
    void testInt64InMin() {
        int64InMin(Long.MIN_VALUE);
    }

    @Test
    void testInt64OutMax() {
        var v = new Out<>(0L);
        int64OutMax(v);
        assertEquals(Long.MAX_VALUE, v.get());
    }

    @Test
    void testInt64OutMin() {
        var v = new Out<>(0L);
        int64OutMin(v);
        assertEquals(Long.MIN_VALUE, v.get());
    }

    @Test
    void testInt64OutUninitialized() {
        var v = new Out<>(0L);
        assertFalse(int64OutUninitialized(v));
        assertEquals(0L, v.get());
    }

    @Test
    void testInt64OutMaxMin() {
        var v = new Out<>(Long.MAX_VALUE);
        int64InoutMaxMin(v);
        assertEquals(Long.MIN_VALUE, v.get());
    }

    @Test
    void testInt64OutMinMax() {
        var v = new Out<>(Long.MIN_VALUE);
        int64InoutMinMax(v);
        assertEquals(Long.MAX_VALUE, v.get());
    }

    @Test
    void testUint64Return() {
        assertEquals(-1L, uint64Return());
    }

    @Test
    void testUint64In() {
        uint64In(-1L);
    }

    @Test
    void testUint64Out() {
        var v = new Out<>(0L);
        uint64Out(v);
        assertEquals(-1L, v.get());
    }

    @Test
    void testUint64OutUninitialized() {
        var v = new Out<>(0L);
        assertFalse(uint64OutUninitialized(v));
        assertEquals(0L, v.get());
    }

    @Test
    void testUint64Inout() {
        var v = new Out<>(-1L);
        uint64Inout(v);
        assertEquals(0L, v.get());
    }
}
