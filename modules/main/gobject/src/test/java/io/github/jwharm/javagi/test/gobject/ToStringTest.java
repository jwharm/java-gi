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

package io.github.jwharm.javagi.test.gobject;

import io.github.jwharm.javagi.gobject.types.Types;
import org.gnome.glib.Variant;
import org.gnome.gobject.Value;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * GValue, GType and GVariant have an extra {@code toString()} method.
 * Test if these methods work as expected.
 */
public class ToStringTest {

    @Test
    public void testValueToString() {
        Value vInt = new Value(Arena.ofAuto()).init(Types.INT);
        vInt.setInt(123);
        assertEquals("123", vInt.toString());
        vInt.unset();

        Value vBool = new Value(Arena.ofAuto()).init(Types.BOOLEAN);
        vBool.setBoolean(true);
        assertEquals("TRUE", vBool.toString());
        vBool.unset();

        Value vStr = new Value(Arena.ofAuto()).init(Types.STRING);
        vStr.setString("abc");
        assertEquals("\"abc\"", vStr.toString());
        vStr.unset();
    }

    @Test
    public void testVariantToString() {
        var vInt = new Variant("u", 40);
        assertEquals("uint32 40", vInt.toString());
    }

    @Test
    public void testTypeToString() {
        // todo
    }
}
