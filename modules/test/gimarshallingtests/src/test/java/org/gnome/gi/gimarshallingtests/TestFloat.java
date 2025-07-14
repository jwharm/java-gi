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

public class TestFloat {
    @Test
    void return_() {
        assertEquals(Float.MAX_VALUE, floatReturn());
    }

    @Test
    void in() {
        floatIn(Float.MAX_VALUE);
    }

    @Test
    void out() {
        var v = new Out<>(0f);
        floatOut(v);
        assertEquals(Float.MAX_VALUE, v.get());
    }

    @Test
    void noncanonicalNanOut() {
        var v = new Out<>(0f);
        floatNoncanonicalNanOut(v);
        assertEquals(Float.NaN, v.get());
    }

    @Test
    void outUninitialized() {
        var v = new Out<>(0f);
        assertFalse(floatOutUninitialized(v));
        assertEquals(0f, v.get());
    }

    @Test
    void inout() {
        var v = new Out<>(Float.MAX_VALUE);
        floatInout(v);
        // Expected value copied from GJS, because GLib G_MINFLOAT is not the
        // same as java.lang.Float.MIN_VALUE
        assertEquals((float) Math.pow(2, -126), v.get());
    }
}
