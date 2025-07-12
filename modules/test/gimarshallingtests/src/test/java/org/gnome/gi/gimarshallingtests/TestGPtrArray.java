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

public class TestGPtrArray {
    private static final String[] TEST_UTF8_ARRAY = {"0", "1", "2"};

    @Test
    void utf8NoneReturn() {
        assertArrayEquals(TEST_UTF8_ARRAY, gptrarrayUtf8NoneReturn());
    }

    @Test
    void utf8ContainerReturn() {
        assertArrayEquals(TEST_UTF8_ARRAY, gptrarrayUtf8ContainerReturn());
    }

    @Test
    void utf8FullReturn() {
        assertArrayEquals(TEST_UTF8_ARRAY, gptrarrayUtf8FullReturn());
    }

    @Test
    void boxedStructFullReturn() {
        BoxedStruct[] structs = gptrarrayBoxedStructFullReturn();
        assertNotNull(structs);
        assertEquals(3, structs.length);
    }

    @Test
    void utf8NoneIn() {
        gptrarrayUtf8NoneIn(TEST_UTF8_ARRAY);
    }

    @Test
    void utf8ContainerIn() {
        gptrarrayUtf8ContainerIn(TEST_UTF8_ARRAY);
    }

    @Test
    void utf8FullIn() {
        gptrarrayUtf8FullIn(TEST_UTF8_ARRAY);
    }
}
