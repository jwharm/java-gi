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

public class TestArrayUtf8Fixed {
    private final String[] IN_VALUES = new String[] { "🅰", "β", "c", "d" };
    private final String[] OUT_VALUES = new String[] { "a", "b", "¢", "🔠" };

    @Test
    void noneReturn() {
        assertArrayEquals(OUT_VALUES, fixedArrayUtf8NoneReturn());
    }

    @Test
    void containerReturn() {
        assertArrayEquals(OUT_VALUES, fixedArrayUtf8ContainerReturn());
    }

    @Test
    void fullReturn() {
        assertArrayEquals(OUT_VALUES, fixedArrayUtf8FullReturn());
    }

    @Test
    void noneIn() {
        fixedArrayUtf8NoneIn(IN_VALUES);
    }

    @Test
    void containerIn() {
        fixedArrayUtf8ContainerIn(IN_VALUES);
    }

    @Test
    void fullIn() {
        fixedArrayUtf8FullIn(IN_VALUES);
    }

    @Test
    void noneOut() {
        var v = new Out<String[]>();
        fixedArrayUtf8NoneOut(v);
        assertArrayEquals(OUT_VALUES, v.get());
    }

    @Test
    void containerOut() {
        var v = new Out<String[]>();
        fixedArrayUtf8ContainerOut(v);
        assertArrayEquals(OUT_VALUES, v.get());
    }

    @Test
    void fullOut() {
        var v = new Out<String[]>();
        fixedArrayUtf8FullOut(v);
        assertArrayEquals(OUT_VALUES, v.get());
    }

    @Test
    void noneInout() {
        var v = new Out<>(IN_VALUES);
        fixedArrayUtf8NoneInout(v);
        assertArrayEquals(OUT_VALUES, v.get());
    }

    @Test
    void containerInout() {
        var v = new Out<>(IN_VALUES);
        fixedArrayUtf8ContainerInout(v);
        assertArrayEquals(OUT_VALUES, v.get());
    }

    @Test
    void fullInout() {
        var v = new Out<>(IN_VALUES);
        fixedArrayUtf8FullInout(v);
        assertArrayEquals(OUT_VALUES, v.get());
    }
}
