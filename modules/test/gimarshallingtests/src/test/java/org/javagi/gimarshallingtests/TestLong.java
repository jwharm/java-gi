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

/*
 * Native long values are cast to int in java-gi, because
 * long is not 64 bits on all supported platforms. So we
 * treat it as a 32 bit value to preserve cross-platform
 * compatibility.
 */
public class TestLong {
    @Test
    void testLongReturnMax() {
        assertEquals(-1, longReturnMax());
    }

    @Test
    void testLongReturnMin() {
        assertEquals(0, longReturnMin());
    }

    // longInMax is not supported

    // longInMin is not supported

    @Test
    void testLongOutMax() {
        var v = new Out<>(0);
        longOutMax(v);
        assertEquals(-1, v.get());
    }

    @Test
    void testLongOutMin() {
        var v = new Out<>(0);
        longOutMin(v);
        assertEquals(0, v.get());
    }

    @Test
    void testLongOutUninitialized() {
        var v = new Out<>(0);
        assertFalse(longOutUninitialized(v));
        assertEquals(0, v.get());
    }

    // longInoutMaxMin is not supported

    // longInoutMinMax is not supported

    @Test
    void testUlongReturn() {
        assertEquals(-1, ulongReturn());
    }

    @Test
    void testUlongIn() {
        ulongIn(-1);
    }

    @Test
    void testUlongOut() {
        var v = new Out<>(0);
        ulongOut(v);
        assertEquals(-1, v.get());
    }

    @Test
    void testUlongOutUninitialized() {
        var v = new Out<>(0);
        assertFalse(ulongOutUninitialized(v));
        assertEquals(0, v.get());
    }

    // ulongInout is not supported
}
