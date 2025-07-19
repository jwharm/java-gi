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

import org.gnome.gobject.Value;
import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestGValue {
    @Test
    void return_() {
        Value v = gvalueReturn();
        assertNotNull(v);
        assertEquals(42, v.getInt());
    }

    @Test
    void noncanonicalNanFloat() {
        Value v = gvalueNoncanonicalNanFloat();
        assertNotNull(v);
        assertEquals(Float.NaN, v.getFloat());
    }

    @Test
    void noncanonicalNanDouble() {
        Value v = gvalueNoncanonicalNanDouble();
        assertNotNull(v);
        assertEquals(Double.NaN, v.getDouble());
    }
}
