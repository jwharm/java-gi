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

import org.gnome.glib.Variant;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestVariant {
    @Test
    void i() {
        var v = testGvariantI();
        assertNotNull(v);
        assertEquals(1, v.unpack());
    }

    @Test
    void s() {
        var v = testGvariantS();
        assertNotNull(v);
        assertEquals("one", v.unpack());
    }

    @Test
    void asv() {
        var v = testGvariantAsv();
        assertNotNull(v);
        assertEquals(Map.of("name", "foo", "timeout", 10), v.unpackRecursive());
    }

    @Test
    void v() {
        var v = testGvariantV();
        assertNotNull(v);
        assertInstanceOf(Variant.class, v.unpack());
        assertInstanceOf(String.class, v.unpackRecursive());
        assertEquals("contents", v.unpackRecursive());
    }

    @Test
    void as() {
        var v = testGvariantAs();
        assertNotNull(v);
        assertEquals(List.of("one", "two", "three"), v.unpackRecursive());
    }
}
