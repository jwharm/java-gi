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
import org.javagi.base.GErrorException;
import org.javagi.base.Out;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestAbstractDrawableObject {
    public static class MyDrawable extends TestInheritDrawable {
    }

    MyDrawable o;

    @BeforeEach
    void init() {
        o = new MyDrawable();
    }

    @Test
    void doFoo() {
        o.doFoo(42);
    }

    @Test
    void getOrigin() {
        var x = new Out<Integer>();
        var y = new Out<Integer>();
        o.getOrigin(x, y);
        assertEquals(0, x.get());
        assertEquals(0, y.get());
    }

    @Test
    void getSize() {
        var width = new Out<Integer>();
        var height = new Out<Integer>();
        o.getSize(width, height);
        assertEquals(42, width.get());
        assertEquals(42, height.get());
    }

    @Test
    void doFooMaybeThrow() {
        assertDoesNotThrow(() -> o.doFooMaybeThrow(42));
        assertThrows(GErrorException.class, () -> o.doFooMaybeThrow(43));
    }
}
