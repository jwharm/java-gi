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

import org.gnome.gi.gimarshallingtests.BoxedStruct;
import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestArrayZeroTerminated {
    private final int[] TEST_INT_ARRAY = {-1, 0, 1, 2};
    private final int[] TEST_UCS4_ARRAY = {0x63, 0x6f, 0x6e, 0x73, 0x74, 0x20,
            0x2665, 0x20, 0x75, 0x74, 0x66, 0x38};

    @Test
    void return_() {
        assertArrayEquals(new String[] {"0", "1", "2"}, arrayZeroTerminatedReturn());
    }

    @Test
    void returnNull() {
        assertNull(arrayZeroTerminatedReturnNull());
    }

    @Test
    void returnStruct() {
        BoxedStruct[] structs = arrayZeroTerminatedReturnStruct();
        assertNotNull(structs);
        assertEquals(3, structs.length);
    }

    @Test
    void returnUnichar() {
        int[] array = arrayZeroTerminatedReturnUnichar();
        assertArrayEquals(TEST_UCS4_ARRAY, array);
    }

    @Test
    void returnUnaligned() {
        byte[] array = arrayZeroTerminatedReturnUnaligned();
        assertNotNull(array);
        assertEquals(7, array.length);
        assertArrayEquals(new byte[] {1, 2, 3, 4}, Arrays.copyOfRange(array, 0, 4));
        cleanupUnalignedBuffer();
    }

    @Test
    void in() {
        arrayZeroTerminatedIn(new String[] {"0", "1", "2"});
    }

    @Test
    void out() {
        var v = new Out<String[]>();
        arrayZeroTerminatedOut(v);
        assertArrayEquals(new String[] {"0", "1", "2"}, v.get());
    }

    @Test
    void outUninitialized() {
        assertDoesNotThrow(() -> arrayZeroTerminatedOutUninitialized(null));
        var v = new Out<>(TEST_INT_ARRAY);
        assertThrows(NullPointerException.class, () -> arrayOutUninitialized(v));
    }

    @Test
    void outUnaligned() {
        var v = new Out<byte[]>();
        arrayZeroTerminatedOutUnaligned(v);
        byte[] array = v.get();
        assertNotNull(array);
        assertEquals(7, array.length);
        assertArrayEquals(new byte[] {1, 2, 3, 4}, Arrays.copyOfRange(array, 0, 4));
        cleanupUnalignedBuffer();
    }

    @Test
    void inout() {
        var array = new Out<>(new String[] {"0", "1", "2"});
        arrayZeroTerminatedInout(array);
        assertArrayEquals(new String[] {"-1", "0", "1", "2"}, array.get());
    }

}
