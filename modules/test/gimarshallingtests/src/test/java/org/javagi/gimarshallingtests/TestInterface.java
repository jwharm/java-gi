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

import org.gnome.gi.gimarshallingtests.Interface2;
import org.gnome.gi.gimarshallingtests.Interface3;
import org.gnome.gi.gimarshallingtests.InterfaceImpl;
import org.gnome.glib.Variant;
import org.gnome.gobject.GObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestInterface {
    @Test
    void canBeReturned() {
        var ifaceImpl = new InterfaceImpl();
        var itself = ifaceImpl.getAsInterface();
        assertEquals(itself, ifaceImpl);
    }

    @Test
    void canCallAndInterfaceVFuncInC() {
        var ifaceImpl = new InterfaceImpl();
        ifaceImpl.testInt8In((byte) 42);
        testInterfaceTestInt8In(ifaceImpl, (byte) 42);
    }

    static class I2Impl extends GObject implements Interface2 {
    }

    @Test
    void canImplementACInterface() {
        assertDoesNotThrow(I2Impl::new);
    }

    static class I3Impl extends GObject implements Interface3 {
        ArrayList<String> stuff;
        @Override
        public void testVariantArrayIn(Variant[] variantArray) {
            stuff = new ArrayList<>();
            for (Variant v : variantArray) {
                stuff.add(v.toString());
            }
        }
    }

    @Test
    void canImplementACInterfaceWithAVFunc() {
        var i3 = new I3Impl();
        i3.testVariantArrayIn(new Variant[] {
                new Variant("b", true),
                new Variant("s", "hello"),
                new Variant("i", 42),
                new Variant("t", 43),
                new Variant("x", 44),
        });
        assertIterableEquals(
                List.of("true", "'hello'", "42", "uint64 43", "int64 44"),
                i3.stuff);
    }
}
