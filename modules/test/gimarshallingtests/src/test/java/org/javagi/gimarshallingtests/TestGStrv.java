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

public class TestGStrv {
    private static final String[] TEST_UTF8_ARRAY = {"0", "1", "2"};

    @Test
    void return_() {
        assertArrayEquals(TEST_UTF8_ARRAY, gstrvReturn());
    }

    @Test
    void in() {
        gstrvIn(TEST_UTF8_ARRAY);
    }

    @Test
    void out() {
        var v = new Out<String[]>();
        gstrvOut(v);
        assertArrayEquals(TEST_UTF8_ARRAY, v.get());
    }

    @Test
    void outUninitialized() {
        var v = new Out<String[]>();
        assertFalse(gstrvOutUninitialized(v));
        assertNull(v.get());
    }

    @Test
    void inout() {
        var v = new Out<>(TEST_UTF8_ARRAY);
        gstrvInout(v);
        assertArrayEquals(new String[] {"-1", "0", "1", "2"}, v.get());
    }
}
