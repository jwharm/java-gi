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

import org.gnome.gi.gimarshallingtests.Enum;
import org.gnome.gi.gimarshallingtests.GEnum;
import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestEnum {
    @Test
    void genumReturnv() {
        assertEquals(GEnum.VALUE3, GEnum.returnv());
    }

    @Test
    void genumOut() {
        var v = new Out<GEnum>();
        GEnum.out(v);
        assertEquals(GEnum.VALUE3, v.get());
    }

    @Test
    void genumOutUninitialized() {
        var v = new Out<GEnum>();
        assertFalse(GEnum.outUninitialized(v));
    }

    @Test
    void returnv() {
        assertEquals(Enum.VALUE3, enumReturnv());
    }

    @Test
    void out() {
        var v = new Out<Enum>();
        enumOut(v);
        assertEquals(Enum.VALUE3, v.get());
    }

    @Test
    void outUninitialized() {
        var v = new Out<Enum>();
        assertFalse(enumOutUninitialized(v));
    }

    @Test
    void inout() {
        var v = new Out<>(Enum.VALUE3);
        enumInout(v);
        assertEquals(Enum.VALUE1, v.get());
    }
}
