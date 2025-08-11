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

public class TestReturnValueCombinations {
    @Test
    void testIntOutOut() {
        var int0 = new Out<Integer>();
        var int1 = new Out<Integer>();
        intOutOut(int0, int1);
        assertEquals(6, int0.get());
        assertEquals(7, int1.get());
    }

    @Test
    void testIntThreeInThreeOut() {
        var out0 = new Out<Integer>();
        var out1 = new Out<Integer>();
        var out2 = new Out<Integer>();
        intThreeInThreeOut(1, 2, 3, out0, out1, out2);
        assertEquals(1, out0.get());
        assertEquals(2, out1.get());
        assertEquals(3, out2.get());
    }

    @Test
    void testIntReturnOut() {
        var v = new Out<Integer>();
        int ret = intReturnOut(v);
        assertEquals(6, ret);
        assertEquals(7, v.get());
    }

    @Test
    void testFourParameters() {
        intTwoInUtf8TwoInWithAllowNone(1, 2, "3", "4");
        intTwoInUtf8TwoInWithAllowNone(1, 2, "3", null);
        intTwoInUtf8TwoInWithAllowNone(1, 2, null, "4");
        intTwoInUtf8TwoInWithAllowNone(1, 2, null, null);
    }

    @Test
    void testThreeParameters() {
        intOneInUtf8TwoInOneAllowsNone(1, "2", "3");
        intOneInUtf8TwoInOneAllowsNone(1, null, "3");
    }

    @Test
    void testArrayAndTwoParameters() {
        int[] array = new int[] {-1, 0, 1, 2};
        arrayInUtf8TwoIn(array, "1", "2");
        arrayInUtf8TwoIn(array, "1", null);
        arrayInUtf8TwoIn(array, null, "2");
        arrayInUtf8TwoIn(array, null, null);
    }


    @Test
    void testArrayAndTwoParametersOutOfOrder() {
        int[] array = new int[] {-1, 0, 1, 2};
        arrayInUtf8TwoInOutOfOrder("1", array, "2");
        arrayInUtf8TwoInOutOfOrder("1", array, null);
        arrayInUtf8TwoInOutOfOrder(null, array, "2");
        arrayInUtf8TwoInOutOfOrder(null, array, null);
    }
}
