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

import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestArguments {
    @Test
    void inAfterOut() {
        var str = "hello";
        var len = new Out<Integer>();
        testIntOutUtf8(len, str);
        assertEquals(str.length(), len.get());
    }

    @Test
    void multipleNumberArgs() {
        var times2 = new Out<Double>();
        var times3 = new Out<Double>();
        testMultiDoubleArgs(2.5, times2, times3);
        assertEquals(5.0, times2.get());
        assertEquals(7.5, times3.get());
    }

    @Test
    void multipleStringOutArgs() {
        var first = new Out<String>();
        var second = new Out<String>();
        testUtf8OutOut(first, second);
        assertEquals("first", first.get());
        assertEquals("second", second.get());
    }

    @Test
    void stringsAsReturnAndOutArg() {
        var second = new Out<String>();
        String first = testUtf8OutNonconstReturn(second);
        assertEquals("first", first);
        assertEquals("second", second.get());
    }

    @Test
    void nullableStringInArg() {
        testUtf8NullIn(null);
    }

    @Test
    void nullableStringOutArg() {
        var out = new Out<String>();
        testUtf8NullOut(out);
        assertNull(out.get());
    }
}
