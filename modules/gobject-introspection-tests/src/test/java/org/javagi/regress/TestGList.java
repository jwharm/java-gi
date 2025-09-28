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

import org.gnome.gi.regress.TestObj;
import org.gnome.gi.regress.TestSubObj;
import org.gnome.glib.Type;
import org.javagi.base.Out;
import org.javagi.base.TransferOwnership;
import org.javagi.interop.Interop;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.function.Function;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestGList {
    private static final List<String> STR_LIST = List.of("1", "2", "3");

    @Test
    void noneReturn() {
        assertIterableEquals(STR_LIST, testGlistNothingReturn());
        assertIterableEquals(STR_LIST, testGlistNothingReturn2());
    }

    @Test
    void containerReturn() {
        assertIterableEquals(STR_LIST, testGlistContainerReturn());
    }

    @Test
    void fullReturn() {
        assertIterableEquals(STR_LIST, testGlistEverythingReturn());
    }

    @Test
    void noneIn() {
        var list = new org.gnome.glib.List<>(Interop::getStringFrom, null, TransferOwnership.NONE);
        list.addAll(STR_LIST);
        testGlistNothingIn(list);
        testGlistNothingIn2(list);
    }

    @Test
    void nullIn() {
        testGlistNullIn(null);
    }

    @Test
    void nullOut() {
        var out = new Out<org.gnome.glib.List<String>>();
        testGlistNullOut(out);
        assertNotNull(out.get());
        assertTrue(out.get().isEmpty());
    }

    @Test
    void containerIn() {
        Function<MemorySegment, Type> make = addr -> new Type(addr.address());
        var list = new org.gnome.glib.List<>(make, null, TransferOwnership.CONTAINER);
        list.add(TestObj.getType());
        list.add(TestSubObj.getType());
        testGlistGtypeContainerIn(list);
    }
}
