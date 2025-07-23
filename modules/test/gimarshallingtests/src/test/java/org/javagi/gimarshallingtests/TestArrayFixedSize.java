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

import org.gnome.gi.gimarshallingtests.SimpleStruct;
import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestArrayFixedSize {
    private final int[] TEST_INT_ARRAY = {-1, 0, 1, 2};
    private final short[] TEST_SHORT_ARRAY = {-1, 0, 1, 2};

    @Test
    void intReturn() {
        assertArrayEquals(TEST_INT_ARRAY, arrayFixedIntReturn());
    }

    @Test
    void shortReturn() {
        assertArrayEquals(TEST_SHORT_ARRAY, arrayFixedShortReturn());
    }

    @Test
    void returnUnaligned() {
        byte[] array = arrayFixedReturnUnaligned();
        assertNotNull(array);
        assertEquals(32, array.length);
        assertArrayEquals(new byte[] {1, 2, 3, 4}, Arrays.copyOfRange(array, 0, 4));
        cleanupUnalignedBuffer();
    }

    @Test
    void intIn() {
        arrayFixedIntIn(TEST_INT_ARRAY);
    }

    @Test
    void shortIn() {
        arrayFixedShortIn(TEST_SHORT_ARRAY);
    }

    @Test
    void callerAllocatedOut() {
        var v = new Out<int[]>();
        arrayFixedCallerAllocatedOut(v);
        assertArrayEquals(TEST_INT_ARRAY, v.get());
    }

    @Test
    void out() {
        var v = new Out<int[]>();
        arrayFixedOut(v);
        assertArrayEquals(TEST_INT_ARRAY, v.get());
    }

    @Test
    void outUninitialized() {
        assertDoesNotThrow(() -> arrayFixedOutUninitialized(null));
        var v = new Out<>(TEST_INT_ARRAY);
        assertThrows(NullPointerException.class, () -> arrayFixedOutUninitialized(v));
    }

    @Test
    void outUnaligned() {
        var v = new Out<byte[]>();
        arrayFixedOutUnaligned(v);
        byte[] array = v.get();
        assertNotNull(array);
        assertEquals(32, array.length);
        assertArrayEquals(new byte[] {1, 2, 3, 4}, Arrays.copyOfRange(array, 0, 4));
        cleanupUnalignedBuffer();
    }

    @Test
    void outStruct() {
        var v = new Out<SimpleStruct[]>();
        arrayFixedOutStruct(v);
        SimpleStruct[] structs = v.get();
        assertNotNull(structs);
        assertEquals(2, structs.length);
    }

    @Test
    void outStructUninitialized() {
        var v = new Out<SimpleStruct>();
        assertFalse(arrayFixedOutStructUninitialized(v));
        assertNull(v.get());
    }

    @Test
    void outStructCallerAllocated() {
        var v = new Out<SimpleStruct[]>();
        arrayFixedCallerAllocatedStructOut(v);
        SimpleStruct[] array = v.get();
        assertNotNull(array);
        assertEquals(4, array.length);
        assertEquals(-2, array[0].readLong());
        assertEquals(-1, array[0].readInt8());
        assertEquals(1, array[1].readLong());
        assertEquals(2, array[1].readInt8());
        assertEquals(3, array[2].readLong());
        assertEquals(4, array[2].readInt8());
        assertEquals(5, array[3].readLong());
        assertEquals(6, array[3].readInt8());
    }

    @Test
    void inout() {
        var v = new Out<>(TEST_INT_ARRAY);
        arrayFixedInout(v);
        assertArrayEquals(new int[] {2, 1, 0, -1}, v.get());
    }
}
