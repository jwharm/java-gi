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
import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestEnumAndFlags {
    @Test
    void correctEnumValues() {
        assertEquals(4, TestReferenceEnum.ZERO.getValue());
        assertEquals(2, TestReferenceEnum.ONE.getValue());
        assertEquals(54, TestReferenceEnum.TWO.getValue());
        assertEquals(4, TestReferenceEnum.THREE.getValue());
        assertEquals(216, TestReferenceEnum.FOUR.getValue());
        assertEquals(-217, TestReferenceEnum.FIVE.getValue());
    }

    @Test
    void unregisteredEnum() {
        assertEquals(0, TestEnumNoGEnum.EVALUE1.getValue());
        assertEquals(42, TestEnumNoGEnum.EVALUE2.getValue());
        assertEquals('0', TestEnumNoGEnum.EVALUE3.getValue());
    }

    @Test
    void enumParam() {
        assertEquals("value1", TestEnum.VALUE1.param());
        assertEquals("value3", TestEnum.VALUE3.param());
        assertEquals("value1", testUnsignedEnumParam(TestEnumUnsigned.VALUE1));
        assertEquals("value2", testUnsignedEnumParam(TestEnumUnsigned.VALUE2));
    }

    @Test
    void flagsParam() {
        var out = new Out<Set<TestFlags>>();
        globalGetFlagsOut(out);
        assertNotNull(out.get());
        assertEquals(EnumSet.of(TestFlags.FLAG1, TestFlags.FLAG3), out.get());
    }

    @Test
    void testFlagValues() {
        assertEquals(512, TestDiscontinuousFlags.DISCONTINUOUS1.getValue());
        assertEquals(
                EnumSet.of(TestDiscontinuousFlags.DISCONTINUOUS1),
                testDiscontinuous1WithPrivateValues());

        assertEquals(536870912, TestDiscontinuousFlags.DISCONTINUOUS2.getValue());
        assertEquals(
                EnumSet.of(TestDiscontinuousFlags.DISCONTINUOUS2),
                testDiscontinuous2WithPrivateValues());
    }
}
