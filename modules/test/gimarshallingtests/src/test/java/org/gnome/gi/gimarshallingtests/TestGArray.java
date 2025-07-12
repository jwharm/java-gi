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

import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestGArray {
    private final int[] TEST_INT_ARRAY = {-1, 0, 1, 2};

    // G_MAXUINT64 becomes -1 when cast to Java's (signed) long
    private final long[] TEST_UINT64_ARRAY = {0, -1};

    private final String[] TEST_UTF8_ARRAY = {"0", "1", "2"};

    @Test
    void intNoneReturn() {
        assertArrayEquals(TEST_INT_ARRAY, garrayIntNoneReturn());
    }

    @Test
    void uint64NoneReturn() {
        assertArrayEquals(TEST_UINT64_ARRAY, garrayUint64NoneReturn());
    }

    @Test
    void utf8NoneReturn() {
        assertArrayEquals(TEST_UTF8_ARRAY, garrayUtf8NoneReturn());
    }


    @Test
    void utf8ContainerReturn() {
        assertArrayEquals(TEST_UTF8_ARRAY, garrayUtf8ContainerReturn());
    }

    @Test
    void utf8FullReturn() {
        assertArrayEquals(TEST_UTF8_ARRAY, garrayUtf8FullReturn());
    }

    @Test
    void boxedStructFullReturn() {
        BoxedStruct[] structs = garrayBoxedStructFullReturn();
        assertNotNull(structs);
        assertEquals(3, structs.length);
    }

    @Test
    void intNoneIn() {
        garrayIntNoneIn(TEST_INT_ARRAY);
    }

    @Test
    void uint64NoneIn() {
        garrayUint64NoneIn(TEST_UINT64_ARRAY);
    }

    @Test
    void utf8NoneIn() {
        garrayUtf8NoneIn(TEST_UTF8_ARRAY);
    }

    @Test
    void utf8ContainerIn() {
        garrayUtf8ContainerIn(TEST_UTF8_ARRAY);
    }

    @Test
    void utf8FullIn() {
        garrayUtf8FullIn(TEST_UTF8_ARRAY);
    }
}
