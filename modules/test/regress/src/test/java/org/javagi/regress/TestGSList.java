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

import org.gnome.glib.SList;
import org.javagi.base.Out;
import org.javagi.base.TransferOwnership;
import org.javagi.interop.Interop;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestGSList {
    private static final List<String> STR_LIST = List.of("1", "2", "3");

    @Test
    void noneReturn() {
        assertIterableEquals(STR_LIST, testGslistNothingReturn());
        assertIterableEquals(STR_LIST, testGslistNothingReturn2());
    }

    @Test
    void containerReturn() {
        assertIterableEquals(STR_LIST, testGslistContainerReturn());
    }

    @Test
    void fullReturn() {
        assertIterableEquals(STR_LIST, testGslistEverythingReturn());
    }

    @Test
    void noneIn() {
        var list = new org.gnome.glib.SList<>(Interop::getStringFrom, null, TransferOwnership.NONE);
        list.addAll(STR_LIST);
        testGslistNothingIn(list);
        testGslistNothingIn2(list);
    }

    @Test
    void nullIn() {
        testGslistNullIn(null);
    }

    @Test
    void nullOut() {
        var out = new Out<SList<String>>();
        testGslistNullOut(out);
        assertNotNull(out.get());
        assertTrue(out.get().isEmpty());
    }
}
