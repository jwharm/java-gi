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

public class TestArrayGStrvFixedSize {
    private static final String[][] TEST_STRINGS_ARRAY = {
            new String[] {"0", "1", "2"},
            new String[] {"3", "4", "5"},
            new String[] {"6", "7", "8"}
    };

    private static final String[][] TEST_STRINGS_ARRAY_OUT = {
            new String[] {"-1", "0", "1", "2"},
            new String[] {"-1", "3", "4", "5"},
            new String[] {"-1", "6", "7", "8"}
    };

    private void assertArrayEqualsTestArray(String[][] testArray, String[][] arrays) {
        assertNotNull(arrays);
        assertEquals(testArray.length, arrays.length);
        for (int i = 0; i < arrays.length; i++)
            assertArrayEquals(testArray[i], arrays[i]);
    }

    @Test
    void transferFullReturn() {
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY, fixedArrayOfGstrvTransferFullReturn());
    }

    @Test
    void transferContainerReturn() {
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY, fixedArrayOfGstrvTransferContainerReturn());
    }

    @Test
    void transferNoneReturn() {
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY, fixedArrayOfGstrvTransferNoneReturn());
    }

    @Test
    void transferNoneIn() {
        fixedArrayOfGstrvTransferNoneIn(TEST_STRINGS_ARRAY);
    }

    @Test
    void transferContainerIn() {
        fixedArrayOfGstrvTransferContainerIn(TEST_STRINGS_ARRAY);
    }

    @Test
    void transferFullIn() {
        fixedArrayOfGstrvTransferFullIn(TEST_STRINGS_ARRAY);
    }

    @Test
    void transferNoneOut() {
        var v = new Out<String[][]>();
        fixedArrayOfGstrvTransferNoneOut(v);
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY, v.get());
    }

    @Test
    void transferContainerOut() {
        var v = new Out<String[][]>();
        fixedArrayOfGstrvTransferContainerOut(v);
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY, v.get());
    }

    @Test
    void transferFullOut() {
        var v = new Out<String[][]>();
        fixedArrayOfGstrvTransferFullOut(v);
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY, v.get());
    }

    @Test
    void transferFullInout() {
        var v = new Out<>(TEST_STRINGS_ARRAY);
        fixedArrayOfGstrvTransferFullInout(v);
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY_OUT, v.get());
    }

    @Test
    void transferNoneInout() {
        var v = new Out<>(TEST_STRINGS_ARRAY);
        fixedArrayOfGstrvTransferNoneInout(v);
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY_OUT, v.get());
    }

    @Test
    void transferContainerInout() {
        var v = new Out<>(TEST_STRINGS_ARRAY);
        fixedArrayOfGstrvTransferContainerInout(v);
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY_OUT, v.get());
    }
}
