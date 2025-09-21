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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestObjectInherited {
    @Test
    void canBeConstructed() {
        var subobj = new TestSubObj();
        assertEquals(TestSubObj.getType(), subobj.readGClass().readGType());
    }

    @Test
    void callOverridedInstanceMethod() {
        var subobj = new TestSubObj();
        assertEquals(0, subobj.instanceMethod());
    }

    @Test
    void hasProperties() {
        var subobj = TestSubObj.builder().setBoolean(true).build();
        assertTrue((boolean) subobj.getProperty("boolean"));
        subobj.setProperty("boolean", false);
        assertFalse((boolean) subobj.getProperty("boolean"));
    }

    @Test
    void overriddenInterfaceProperties() {
        var subobj1 = TestSubObj.builder().setNumber(4).build();
        assertEquals(4, subobj1.getProperty("number"));

        var subobj2 = new TestSubObj();
        assertEquals(0, subobj2.getProperty("number"));

        subobj2.setProperty("number", 4);
        assertEquals(4, subobj2.getProperty("number"));
    }
}
