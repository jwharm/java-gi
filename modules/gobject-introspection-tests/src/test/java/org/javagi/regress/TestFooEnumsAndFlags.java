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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestFooEnumsAndFlags {
    @Test
    void enumType() {
        assertEquals(0, Regress.fooEnumMethod(FooEnumType.BETA));
        assertEquals(1, FooEnumType.ALPHA.method());
        assertEquals(FooEnumType.DELTA, FooEnumType.returnv(1));
    }

    @Test
    void flagsType() {
        assertEquals(1, FooFlagsType.FIRST.getValue());
        assertEquals(2, FooFlagsType.SECOND.getValue());
        assertEquals(4, FooFlagsType.THIRD.getValue());
        assertEquals(6, Regress.FOO_FLAGS_SECOND_AND_THIRD);
    }

    @Test
    void enumNoType() {
        assertEquals(1, FooEnumNoType.UN.getValue());
        assertEquals(2, FooEnumNoType.DEUX.getValue());
        assertEquals(3, FooEnumNoType.TROIS.getValue());
        assertEquals(9, FooEnumNoType.NEUF.getValue());
    }

    @Test
    void flagsNoType() {
        assertEquals(1, FooFlagsNoType.ETT.getValue());
        assertEquals(2, FooFlagsNoType.TVA.getValue());
        assertEquals(4, FooFlagsNoType.FYRA.getValue());
    }

    @Test
    void enumFullName() {
        assertEquals(1, FooEnumFullname.ONE.getValue());
        assertEquals(2, FooEnumFullname.TWO.getValue());
        assertEquals(3, FooEnumFullname.THREE.getValue());
    }

    @Test
    void addressType() {
        assertEquals(0, FooAddressType.INVALID.getValue());
        assertEquals(1, FooAddressType.IPV4.getValue());
        assertEquals(2, FooAddressType.IPV6.getValue());
    }
}
