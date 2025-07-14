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

import org.gnome.glib.Type;
import org.javagi.gobject.types.Types;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestGType {
    @Test
    void return_() {
        assertEquals(Types.NONE, gtypeReturn());
    }

    @Test
    void stringReturn() {
        assertEquals(Types.STRING, gtypeStringReturn());
    }

    @Test
    void in() {
        gtypeIn(Types.NONE);
    }

    @Test
    void stringIn() {
        gtypeStringIn(Types.STRING);
    }

    @Test
    void out() {
        var gtype = new Type(1);
        gtypeOut(gtype);
        assertEquals(Types.NONE, gtype);
    }

    @Test
    void outUninitialized() {
        var gtype = new Type(Types.STRING.getValue());
        assertFalse(gtypeOutUninitialized(gtype));
        assertEquals(0, gtype.getValue());
    }

    @Test
    void stringOut() {
        var gtype = new Type(1);
        gtypeStringOut(gtype);
        assertEquals(Types.STRING, gtype);
    }

    @Test
    void inOut() {
        var gtype = new Type(Types.NONE.getValue());
        gtypeInout(gtype);
        assertEquals(Types.INT, gtype);
    }
}
