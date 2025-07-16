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

import org.gnome.glib.Variant;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestArrayGVariant {
    private final Variant[] TEST_VARIANT_ARRAY = new Variant[] {
            new Variant("i", 27, null),
            new Variant("s", "Hello", null)
    };

    @Test
    void noneIn() {
        Variant[] result = arrayGvariantNoneIn(TEST_VARIANT_ARRAY);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(27, result[0].getInt32());
        assertEquals("Hello", result[1].getString(null));
    }

    @Test
    void containerIn() {
        Variant[] result = arrayGvariantContainerIn(TEST_VARIANT_ARRAY);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(27, result[0].getInt32());
        assertEquals("Hello", result[1].getString(null));
    }

    @Test
    void fullIn() {
        Variant[] result = arrayGvariantFullIn(TEST_VARIANT_ARRAY);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(27, result[0].getInt32());
        assertEquals("Hello", result[1].getString(null));
    }
}
