/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2026 Jan-Willem Harmannij
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

package org.javagi.glib;

import org.gnome.glib.DebugKey;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test allocation of a struct with different types of Arena.
 */
public class StructTest {
    @Test
    void confinedArena() {
        try (Arena arena = Arena.ofConfined()) {
            DebugKey struct = new DebugKey("abc", 123, arena);
            System.gc();
            assertEquals(123, struct.readValue());
            assertEquals("abc", struct.readKey());
        }
    }

    @Test
    void autoArena() {
        DebugKey struct = new DebugKey("abc", 123);
        System.gc();
        assertEquals(123, struct.readValue());
        assertEquals("abc", struct.readKey());
    }
}
