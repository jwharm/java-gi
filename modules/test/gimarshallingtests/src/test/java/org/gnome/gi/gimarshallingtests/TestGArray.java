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
    private static final int[] TEST_INT_ARRAY = {-1, 0, 1, 2};
    // G_MAXUINT64 becomes -1 when cast to Java's (signed) long
    private static final long[] TEST_UINT64_ARRAY = {0, -1};
    private static final String[] TEST_UTF8_ARRAY = {"0", "1", "2"};
    private static final int[] TEST_UCS4_ARRAY = {
            0x63, 0x6f, 0x6e, 0x73, 0x74, 0x20,
            0x2665, 0x20, 0x75, 0x74, 0x66, 0x38};

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

    @Test
    void utf8NoneOut() {
        var v = new Out<String[]>();
        garrayUtf8NoneOut(v);
        assertArrayEquals(TEST_UTF8_ARRAY, v.get());
    }

    @Test
    void utf8NoneOutUninitialized() {
        var v = new Out<String[]>();
        assertThrows(NullPointerException.class, () -> garrayUtf8NoneOutUninitialized(v));
    }

    @Test
    void utf8ContainerOut() {
        var v = new Out<String[]>();
        garrayUtf8ContainerOut(v);
        assertArrayEquals(TEST_UTF8_ARRAY, v.get());
    }

    @Test
    void utf8ContainerOutUninitialized() {
        var v = new Out<String[]>();
        assertThrows(NullPointerException.class, () -> garrayUtf8ContainerOutUninitialized(v));
    }

    @Test
    void utf8FullOut() {
        var v = new Out<String[]>();
        garrayUtf8FullOut(v);
        assertArrayEquals(TEST_UTF8_ARRAY, v.get());
    }

    @Test
    void utf8FullOutUninitialized() {
        var v = new Out<String[]>();
        assertThrows(NullPointerException.class, () -> garrayUtf8FullOutUninitialized(v));
    }

    @Test
    void utf8FullOutCallerAllocated() {
        var v = new Out<String[]>();
        garrayUtf8FullOutCallerAllocated(v);
        assertArrayEquals(TEST_UTF8_ARRAY, v.get());
    }

    @Test
    void utf8NoneInout() {
        var v = new Out<>(TEST_UTF8_ARRAY);
        garrayUtf8NoneInout(v);
        assertArrayEquals(new String[] {"-2", "-1", "0", "1"}, v.get());
    }

    @Test
    void utf8ContainerInout() {
        var v = new Out<>(TEST_UTF8_ARRAY);
        garrayUtf8ContainerInout(v);
        assertArrayEquals(new String[] {"-2", "-1", "0", "1"}, v.get());
    }

    @Test
    void utf8FullInout() {
        var v = new Out<>(TEST_UTF8_ARRAY);
        garrayUtf8FullInout(v);
        assertArrayEquals(new String[] {"-2", "-1", "0", "1"}, v.get());
    }

    @Test
    void boolNoneIn() {
        garrayBoolNoneIn(new boolean[] {true, false, true, true});
    }

    @Test
    void unicharNoneIn() {
        garrayUnicharNoneIn(TEST_UCS4_ARRAY);
    }
}
