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

import java.util.List;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestUtf8 {
    private static final String CONST_STR = "const ♥ utf8";
    private static final String NONCONST_STR = "nonconst ♥ utf8";

    @Test
    void constReturn() {
        assertEquals(CONST_STR, testUtf8ConstReturn());
    }

    @Test
    void nonconstReturn() {
        assertEquals(NONCONST_STR, testUtf8NonconstReturn());
    }

    @Test
    void constIn() {
        testUtf8ConstIn(CONST_STR);
    }

    @Test
    void out() {
        var out = new Out<String>();
        testUtf8Out(out);
        assertEquals(NONCONST_STR, out.get());
    }

    @Test
    void inout() {
        var inout = new Out<>(CONST_STR);
        testUtf8Inout(inout);
        assertEquals(NONCONST_STR, inout.get());
    }

    @Test
    void filenameReturn() {
        assertEquals(List.of("åäö", "/etc/fstab"), testFilenameReturn());
    }
}
