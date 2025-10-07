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

package org.javagi.regress;

import org.junit.jupiter.api.Test;

import static org.gnome.gi.regressunix.RegressUnix.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestUnixTypes {
    @Test
    void devt() {
        assertEquals(12345, testDevt(12345));
    }

    @Test
    void gidt() {
        assertEquals(12345, testGidt(12345));
    }

    @Test
    void pidt() {
        assertEquals(12345, testPidt(12345));
    }

    @Test
    void socklent() {
        assertEquals(12345, testSocklent(12345));
    }

    @Test
    void uidt() {
        assertEquals(12345, testUidt(12345));
    }
}
