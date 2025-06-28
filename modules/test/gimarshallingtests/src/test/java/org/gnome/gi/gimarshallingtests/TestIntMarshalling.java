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

public class TestIntMarshalling {
    @Test
    void testIntReturnMax() {
        assertEquals(Integer.MAX_VALUE, intReturnMax());
    }

    @Test
    void testIntReturnMin() {
        assertEquals(Integer.MIN_VALUE, intReturnMin());
    }

    @Test
    void testIntInMax() {
        intInMax(Integer.MAX_VALUE);
    }

    @Test
    void testIntInMin() {
        intInMin(Integer.MIN_VALUE);
    }

    @Test
    void testIntOutMax() {
        var v = new Out<>(0);
        intOutMax(v);
        assertEquals(Integer.MAX_VALUE, v.get());
    }

    @Test
    void testIntOutMin() {
        var v = new Out<>(0);
        intOutMin(v);
        assertEquals(Integer.MIN_VALUE, v.get());
    }

    @Test
    void testIntOutUninitialized() {
        var v = new Out<>(0);
        assertFalse(intOutUninitialized(v));
        assertEquals(0, v.get());
    }

    @Test
    void testIntOutMaxMin() {
        var v = new Out<>(Integer.MAX_VALUE);
        intInoutMaxMin(v);
        assertEquals(Integer.MIN_VALUE, v.get());
    }

    @Test
    void testIntOutMinMax() {
        var v = new Out<>(Integer.MIN_VALUE);
        intInoutMinMax(v);
        assertEquals(Integer.MAX_VALUE, v.get());
    }

    @Test
    void testUintReturn() {
        assertEquals(-1, uintReturn());
    }

    @Test
    void testUintIn() {
        uintIn(-1);
    }

    @Test
    void testUintOut() {
        var v = new Out<>(0);
        uintOut(v);
        assertEquals(-1, v.get());
    }

    @Test
    void testUintOutUninitialized() {
        var v = new Out<>(0);
        assertFalse(uintOutUninitialized(v));
        assertEquals(0, v.get());
    }

    @Test
    void testUintInout() {
        var v = new Out<>(-1);
        uintInout(v);
        assertEquals(0, v.get());
    }
}
