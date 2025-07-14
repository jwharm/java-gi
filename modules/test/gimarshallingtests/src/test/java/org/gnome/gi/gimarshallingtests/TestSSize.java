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

public class TestSSize {
    @Test
    void testSsizeReturnMax() {
        assertEquals(Long.MAX_VALUE, ssizeReturnMax());
    }

    @Test
    void testSsizeReturnMin() {
        assertEquals(Long.MIN_VALUE, ssizeReturnMin());
    }

    @Test
    void testSsizeInMax() {
        ssizeInMax(Long.MAX_VALUE);
    }

    @Test
    void testSsizeInMin() {
        ssizeInMin(Long.MIN_VALUE);
    }

    @Test
    void testSsizeOutMax() {
        var v = new Out<>(0L);
        ssizeOutMax(v);
        assertEquals(Long.MAX_VALUE, v.get());
    }

    @Test
    void testSsizeOutMin() {
        var v = new Out<>(0L);
        ssizeOutMin(v);
        assertEquals(Long.MIN_VALUE, v.get());
    }

    @Test
    void testSsizeOutUninitialized() {
        var v = new Out<>(0L);
        assertFalse(ssizeOutUninitialized(v));
        assertEquals(0L, v.get());
    }

    @Test
    void testSsizeOutMaxMin() {
        var v = new Out<>(Long.MAX_VALUE);
        ssizeInoutMaxMin(v);
        assertEquals(Long.MIN_VALUE, v.get());
    }

    @Test
    void testSsizeOutMinMax() {
        var v = new Out<>(Long.MIN_VALUE);
        ssizeInoutMinMax(v);
        assertEquals(Long.MAX_VALUE, v.get());
    }

    @Test
    void testSizeReturn() {
        assertEquals(-1L, sizeReturn());
    }

    @Test
    void testSizeIn() {
        sizeIn(-1);
    }

    @Test
    void testSizeOut() {
        var v = new Out<>(0L);
        sizeOut(v);
        assertEquals(-1L, v.get());
    }

    @Test
    void testSizeOutUninitialized() {
        var v = new Out<>(0L);
        assertFalse(sizeOutUninitialized(v));
        assertEquals(0L, v.get());
    }

    @Test
    void testSizeInout() {
        var v = new Out<>(-1L);
        sizeInout(v);
        assertEquals(0L, v.get());
    }
}
