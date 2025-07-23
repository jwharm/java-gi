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
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestUtf8 {

    private static final String CONSTANT_UTF8 = "const ♥ utf8";

    @Test
    void noneReturn() {
        assertEquals(CONSTANT_UTF8, utf8NoneReturn());
    }


    @Test
    void fullReturn() {
        assertEquals(CONSTANT_UTF8, utf8FullReturn());
    }

    @Test
    void noneIn() {
        utf8NoneIn(CONSTANT_UTF8);
    }

    @Test
    void fullIn() {
        utf8FullIn(CONSTANT_UTF8);
    }

    @Test
    void asUint8ArrayIn() {
        utf8AsUint8arrayIn(CONSTANT_UTF8.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void noneOut() {
        var v = new Out<>("");
        utf8NoneOut(v);
        assertEquals(CONSTANT_UTF8, v.get());
    }

    @Test
    void noneOutUninitialized() {
        var v = new Out<>("");
        assertFalse(utf8NoneOutUninitialized(v));
        assertNull(v.get());
    }

    @Test
    void fullOut() {
        var v = new Out<>("");
        utf8FullOut(v);
        assertEquals(CONSTANT_UTF8, v.get());
    }

    @Test
    void danglingOut() {
        var v = new Out<>(CONSTANT_UTF8);
        utf8DanglingOut(v);
        assertNull(v.get());

        v.set(null);
        utf8DanglingOut(v);
        assertNull(v.get());
    }

    @Test
    void noneInout() {
        var v = new Out<>(CONSTANT_UTF8);
        utf8NoneInout(v);
        assertEquals("", v.get());
    }

    @Test
    void fullInout() {
        var v = new Out<>(CONSTANT_UTF8);
        utf8FullInout(v);
        assertEquals("", v.get());
    }
}
