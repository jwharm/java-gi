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

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestCallback {
    @Test
    void returnValueOnly() {
        assertEquals(42, callbackReturnValueOnly(() -> 42));
    }

    @Test
    void oneOutParameter() {
        var v = new Out<Float>();
        callbackOneOutParameter(out -> out.set(43f), v);
        assertEquals(43f, v.get());
    }

    @Test
    void multipleOutParameters() {
        var a = new Out<Float>();
        var b = new Out<Float>();
        callbackMultipleOutParameters((aOut, bOut) -> {
            aOut.set(44f);
            bOut.set(45f);
        }, a, b);
        assertEquals(44f, a.get());
        assertEquals(45f, b.get());
    }

    @Test
    void returnValueAndOneOutParameter() {
        var v = new Out<Integer>();
        assertEquals(47, callbackReturnValueAndOneOutParameter(out -> {
            out.set(46);
            return 47;
        }, v));
        assertEquals(46, v.get());
    }

    @Test
    void returnValueAndMultipleOutParameters() {
        var a = new Out<Integer>();
        var b = new Out<Integer>();
        assertEquals(50, callbackReturnValueAndMultipleOutParameters((aOut, bOut) -> {
            aOut.set(48);
            bOut.set(49);
            return 50;
        }, a, b));
        assertEquals(48, a.get());
        assertEquals(49, b.get());
    }

    @Test
    void ownedBoxed() {
        assertEquals(52, callbackOwnedBoxed(box -> {
            assertEquals(1, box.readLong());
            box.writeLong(52);
        }));
    }
}
