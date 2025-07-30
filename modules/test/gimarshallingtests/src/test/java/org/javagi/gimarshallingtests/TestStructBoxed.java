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

import org.gnome.gi.gimarshallingtests.BoxedStruct;
import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestStructBoxed {
    @Test
    void returnv() {
        BoxedStruct struct = BoxedStruct.returnv();
        assertNotNull(struct);
        assertEquals(42, struct.readLong());
        assertEquals("hello", struct.readString());
        assertArrayEquals(new String[] {"0", "1", "2"}, struct.readGStrv());
    }

    @Test
    void inv() {
        BoxedStruct struct = new BoxedStruct();
        struct.writeLong(42);
        struct.inv();
    }

    @Test
    void out() {
        var v = new Out<BoxedStruct>();
        BoxedStruct.out(v);
        assertNotNull(v.get());
        assertEquals(42, v.get().readLong());
    }

    @Test
    void outUninitialized() {
        var v = new Out<BoxedStruct>();
        assertFalse(BoxedStruct.outUninitialized(v));
    }

    @Test
    void inout() {
        BoxedStruct struct = new BoxedStruct();
        struct.writeLong(42);
        var v = new Out<>(struct);
        BoxedStruct.inout(v);
        assertNotNull(v.get());
        assertEquals(0, v.get().readLong());
    }
}
