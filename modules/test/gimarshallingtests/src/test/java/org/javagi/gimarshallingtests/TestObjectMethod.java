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

import org.gnome.gi.gimarshallingtests.GIMarshallingTestsObject;
import org.javagi.base.Out;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestObjectMethod {
    private final int[] TEST_INT_ARRAY = {-1, 0, 1, 2};

    GIMarshallingTestsObject o;

    @BeforeEach
    void createObject() {
        o = new GIMarshallingTestsObject();
    }

    @Test
    void arrayIn() {
        o.methodArrayIn(TEST_INT_ARRAY);
    }

    @Test
    void arrayOut() {
        var v = new Out<int[]>();
        o.methodArrayOut(v);
        assertArrayEquals(TEST_INT_ARRAY, v.get());
    }

    @Test
    void arrayInout() {
        var v = new Out<>(TEST_INT_ARRAY);
        o.methodArrayInout(v);
        assertArrayEquals(new int[] {-2, -1, 0, 1, 2}, v.get());
    }

    @Test
    void arrayReturn() {
        assertArrayEquals(TEST_INT_ARRAY, o.methodArrayReturn());
    }

    @Test
    void withDefaultImplementation() {
        o.setProperty("int", 42);
        o.methodWithDefaultImplementation((byte) 43);
        assertEquals(43, o.getProperty("int"));
    }
}
