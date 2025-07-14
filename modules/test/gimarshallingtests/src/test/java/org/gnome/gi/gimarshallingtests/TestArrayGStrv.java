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

public class TestArrayGStrv {
    private static final String[][] TEST_STRINGS_ARRAY = {
            new String[] {"0", "1", "2"},
            new String[] {"3", "4", "5"},
            new String[] {"6", "7", "8"}
    };

    private void assertArrayEqualsTestArray(String[][] arrays) {
        assertNotNull(arrays);
        assertEquals(TEST_STRINGS_ARRAY.length, arrays.length);
        for (int i = 0; i < arrays.length; i++)
            assertArrayEquals(TEST_STRINGS_ARRAY[i], arrays[i]);
    }

    @Test
    void transferFullReturn() {
        assertArrayEqualsTestArray(lengthArrayOfGstrvTransferFullReturn());
    }

    @Test
    void transferContainerReturn() {
        assertArrayEqualsTestArray(lengthArrayOfGstrvTransferContainerReturn());
    }

    @Test
    void transferNoneReturn() {
        assertArrayEqualsTestArray(lengthArrayOfGstrvTransferNoneReturn());
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
}
