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

import org.gnome.gi.regress.FooBRect;
import org.gnome.gi.regress.FooBUnion;
import org.gnome.gi.regress.FooBoxed;
import org.gnome.gi.regress.FooRectangle;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestFooBoxedVarious {
    @Test
    void fooBoxedMethod() {
        var o1 = new FooBoxed();
        o1.method();
    }

    @Test
    void fooBRect() {
        var o2 = new FooBRect(1.5, -2.5);
        assertEquals(1.5, o2.readX());
        assertEquals(-2.5, o2.readY());

        var o3 = new FooBRect(-1.4, 2.6);
        assertEquals(-1.4, o3.readX());
        assertEquals(2.6, o3.readY());

        o2.add(o3);
        o3.add(o2);
    }

    @Test
    @Disabled("Not supported")
    void fooBUnion() {
        var u = new FooBUnion();
    }

    @Test
    void rectangleInstance() {
        var o1 = new FooRectangle(0, 0, 10, 10);
        var o2 = new FooRectangle(1, 1, 12, 12);
        o1.add(o2);
        o2.add(o1);
    }
}
