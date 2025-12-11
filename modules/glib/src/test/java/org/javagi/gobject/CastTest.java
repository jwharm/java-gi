/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 Jan-Willem Harmannij
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

package org.javagi.gobject;

import org.gnome.gobject.Binding;
import org.gnome.gobject.GObject;
import org.gnome.gobject.SignalGroup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test the TypeInstance.cast() method
 */
public class CastTest {
    @Test
    void testCast() {
        SignalGroup s1 = new SignalGroup(GObject.getType());

        GObject obj = s1.cast(GObject.class);

        assertThrows(ClassCastException.class, () -> {
            SignalGroup s2 = (SignalGroup) obj;
        });

        assertDoesNotThrow(() -> {
            SignalGroup s3 = obj.cast(SignalGroup.class);
        });

        assertThrows(ClassCastException.class, () -> {
            Binding b = obj.cast(Binding.class);
        });
    }
}
