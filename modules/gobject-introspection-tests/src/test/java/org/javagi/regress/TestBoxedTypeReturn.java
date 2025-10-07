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

import org.gnome.gi.regress.*;
import org.gnome.glib.GLib;
import org.gnome.gobject.Value;
import org.javagi.base.Out;
import org.javagi.gobject.ValueUtil;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestBoxedTypeReturn {
    @Test
    void refcountedBoxedTypeWrapper() {
        var wrapper = new TestBoxedCWrapper();
        var copy = wrapper.copy();
        // TestBoxedC uses refcounting, so the underlying native objects should
        // be the same
        assertNotEquals(copy.handle(), wrapper.handle());
        assertEquals(copy.get().handle(), wrapper.get().handle());
    }

    @Test
    void arrayOfBoxedTypeTransferNoneOutParameter() {
        var out = new Out<TestBoxedC[]>();
        testArrayFixedBoxedNoneOut(out);
        assertEquals(2, out.get().length);
        assertEquals(1, out.get()[0].readRefcount());
        assertEquals(1, out.get()[1].readRefcount());
    }

    @Test
    void boxedGValueOut() {
        int int8 = (int) (Math.random() * (GLib.MAXINT8 - GLib.MININT8) + GLib.MININT8);
        var value = new Value();
        testGvalueOutBoxed(value, int8);
        var out = (TestBoxed) ValueUtil.valueToObject(value);
        assertNotNull(out);
        assertEquals(int8, out.readSomeInt8());
    }

    @Test
    void glistBoxedTypeTransferNoneReturn() {
        assertEquals(2, testGlistBoxedNoneReturn(2).size());
    }

    @Test
    void glistBoxedTypeTransferFullReturn() {
        assertEquals(2, testGlistBoxedFullReturn(2).size());
    }
}
