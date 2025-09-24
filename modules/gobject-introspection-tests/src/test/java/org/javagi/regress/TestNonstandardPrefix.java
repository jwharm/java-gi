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

public class TestNonstandardPrefix {
    @Test
    void construct() {
        var o = new TestWi8021x();
        assertTrue((boolean) o.getProperty("testbool"));

        var o2 = TestWi8021x.builder().setTestbool(false).build();
        assertFalse((boolean) o2.getProperty("testbool"));
        o2.setProperty("testbool", true);
        assertTrue((boolean) o2.getProperty("testbool"));
    }

    @Test
    void callInstanceMethod() {
        var o = new TestWi8021x();
        assertTrue(o.getTestbool());
        o.setTestbool(false);
        assertFalse(o.getTestbool());
    }

    @Test
    void callStaticMethod() {
        assertEquals(42, TestWi8021x.staticMethod(21));
    }
}
