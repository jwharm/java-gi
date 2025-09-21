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

package org.javagi.regress;

import org.gnome.gi.regress.*;
import org.gnome.gobject.GObjects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestFundamentalType {
    @Test
    void constructSubtype() {
        var o = new TestFundamentalSubObject("plop");
        assertEquals(TestFundamentalSubObject.getType(), o.readGClass().readGType());
    }

    @Test
    void constructHiddenType() {
        var o = Regress.testCreateFundamentalHiddenClassInstance();
        assertNotNull(o);
        assertTrue(GObjects.typeIsA(o.readGClass().readGType(), TestFundamentalObject.getType()));
        assertNotEquals(TestFundamentalObject.getType(), o.readGClass().readGType());
    }

    @Test
    void constructNoGetSetFunc() {
        var o = new TestFundamentalObjectNoGetSetFunc("plop");
        assertEquals("plop", o.getData());
    }
}
