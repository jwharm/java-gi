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

public class TestInitFunctionMarshalling {
    @Test
    void marshalsNull() {
        assertTrue(initFunction(null));
    }

    @Test
    void marshalsInoutEmptyArray() {
        var params = new String[] {};
        var v = new Out<>(params);
        assertTrue(initFunction(v));
        assertArrayEquals(params, v.get());
    }

    @Test
    void marshalsInoutArray() {
        var params = new String[] {"--foo", "--bar"};
        var v = new Out<>(params);
        assertTrue(initFunction(v));
        assertArrayEquals(new String[] {"--foo"}, v.get());
    }
}
