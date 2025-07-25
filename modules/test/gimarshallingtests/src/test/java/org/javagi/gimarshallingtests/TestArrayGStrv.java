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

import org.javagi.base.Out;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestArrayGStrv {
    private static final String[][] TEST_STRINGS_ARRAY = {
            new String[] {"0", "1", "2"},
            new String[] {"3", "4", "5"},
            new String[] {"6", "7", "8"}
    };

    private static final String[][] TEST_STRINGS_ARRAY_OUT = {
            new String[] {"-1", "0", "1", "2"},
            new String[] {"-1", "3", "4", "5"},
            new String[] {"-1", "6", "7", "8"},
            new String[] {"-1", "9", "10", "11"}
    };

    private void assertArrayEqualsTestArray(String[][] testArray, String[][] arrays) {
        assertNotNull(arrays);
        assertEquals(testArray.length, arrays.length);
        for (int i = 0; i < arrays.length; i++)
            assertArrayEquals(testArray[i], arrays[i]);
    }

    @Test
    void transferFullReturn() {
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY, lengthArrayOfGstrvTransferFullReturn());
    }

    @Test
    void transferContainerReturn() {
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY, lengthArrayOfGstrvTransferContainerReturn());
    }

    @Test
    void transferNoneReturn() {
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY, lengthArrayOfGstrvTransferNoneReturn());
    }

    @Test
    void transferNoneIn() {
        lengthArrayOfGstrvTransferNoneIn(TEST_STRINGS_ARRAY);
    }

    @Test
    void transferContainerIn() {
        lengthArrayOfGstrvTransferContainerIn(TEST_STRINGS_ARRAY);
    }

    @Test
    void transferFullIn() {
        lengthArrayOfGstrvTransferFullIn(TEST_STRINGS_ARRAY);
    }

    @Test
    void transferNoneOut() {
        var v = new Out<String[][]>();
        lengthArrayOfGstrvTransferNoneOut(v);
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY, v.get());
    }

    @Test
    void transferContainerOut() {
        var v = new Out<String[][]>();
        lengthArrayOfGstrvTransferContainerOut(v);
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY, v.get());
    }

    @Test
    void transferFullOut() {
        var v = new Out<String[][]>();
        lengthArrayOfGstrvTransferFullOut(v);
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY, v.get());
    }

    @Test
    void transferFullInout() {
        var v = new Out<>(TEST_STRINGS_ARRAY);
        lengthArrayOfGstrvTransferFullInout(v);
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY_OUT, v.get());
    }

    @Test
    void transferNoneInout() {
        var v = new Out<>(TEST_STRINGS_ARRAY);
        lengthArrayOfGstrvTransferNoneInout(v);
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY_OUT, v.get());
    }

    // See https://gitlab.gnome.org/GNOME/gobject-introspection-tests/-/merge_requests/20
    @Test @Disabled
    void transferContainerInout() {
        var v = new Out<>(TEST_STRINGS_ARRAY);
        lengthArrayOfGstrvTransferContainerInout(v);
        assertArrayEqualsTestArray(TEST_STRINGS_ARRAY_OUT, v.get());
    }
}
