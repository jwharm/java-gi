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

public class TestShortMarshalling {
    @Test
    void testShortReturnMax() {
        assertEquals(Short.MAX_VALUE, shortReturnMax());
    }

    @Test
    void testShortReturnMin() {
        assertEquals(Short.MIN_VALUE, shortReturnMin());
    }

    @Test
    void testShortInMax() {
        shortInMax(Short.MAX_VALUE);
    }

    @Test
    void testShortInMin() {
        shortInMin(Short.MIN_VALUE);
    }

    @Test
    void testShortOutMax() {
        var v = new Out<>((short) 0);
        shortOutMax(v);
        assertEquals(Short.MAX_VALUE, v.get());
    }

    @Test
    void testShortOutMin() {
        var v = new Out<>((short) 0);
        shortOutMin(v);
        assertEquals(Short.MIN_VALUE, v.get());
    }

    @Test
    void testShortOutUninitialized() {
        var v = new Out<>((short) 0);
        assertFalse(shortOutUninitialized(v));
        assertEquals((short) 0, v.get());
    }

    @Test
    void testShortOutMaxMin() {
        var v = new Out<>(Short.MAX_VALUE);
        shortInoutMaxMin(v);
        assertEquals(Short.MIN_VALUE, v.get());
    }

    @Test
    void testShortOutMinMax() {
        var v = new Out<>(Short.MIN_VALUE);
        shortInoutMinMax(v);
        assertEquals(Short.MAX_VALUE, v.get());
    }

    @Test
    void testUshortReturn() {
        assertEquals((short) -1, ushortReturn());
    }

    @Test
    void testUshortIn() {
        ushortIn((short) -1);
    }

    @Test
    void testUshortOut() {
        var v = new Out<>((short) 0);
        ushortOut(v);
        assertEquals((short) -1, v.get());
    }

    @Test
    void testUshortOutUninitialized() {
        var v = new Out<>((short) 0);
        assertFalse(ushortOutUninitialized(v));
        assertEquals((short) 0, v.get());
    }

    @Test
    void testUshortInout() {
        var v = new Out<>((short) -1);
        ushortInout(v);
        assertEquals((short) 0, v.get());
    }
}
