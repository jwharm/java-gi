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

public class TestDoubleMarshalling {
    @Test
    void return_() {
        assertEquals(Double.MAX_VALUE, doubleReturn());
    }

    @Test
    void in() {
        doubleIn(Double.MAX_VALUE);
    }

    @Test
    void out() {
        var v = new Out<>(0d);
        doubleOut(v);
        assertEquals(Double.MAX_VALUE, v.get());
    }

    @Test
    void noncanonicalNanOut() {
        var v = new Out<>(0d);
        doubleNoncanonicalNanOut(v);
        assertEquals(Double.NaN, v.get());
    }

    @Test
    void outUninitialized() {
        var v = new Out<>(0d);
        assertFalse(doubleOutUninitialized(v));
        assertEquals(0d, v.get());
    }

    @Test
    void inout() {
        var v = new Out<>(Double.MAX_VALUE);
        doubleInout(v);
        // Expected value copied from GJS:
        // GLib G_MINDOUBLE is the minimum normal value, which is not the same
        // as the minimum denormal value java.lang.Float.MIN_VALUE
        assertEquals(Math.pow(2, -1022), v.get());
    }
}
