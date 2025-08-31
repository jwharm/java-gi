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

public class TestBoxedStruct {
    @Test
    void createSimple() {
        var struct = new TestSimpleBoxedA(42, (byte) 43, 42.5, TestEnum.VALUE3);
        assertEquals(42, struct.readSomeInt());
        assertEquals(43, struct.readSomeInt8());
        assertEquals(42.5, struct.readSomeDouble());
        assertEquals(TestEnum.VALUE3, struct.readSomeEnum());
    }

    @Test
    void returnSimple() {
        var struct = TestSimpleBoxedA.constReturn();
        assertNotNull(struct);
        assertEquals(5, struct.readSomeInt());
        assertEquals(6, struct.readSomeInt8());
        assertEquals(7, struct.readSomeDouble());
    }

    @Test
    void createNested() {
        var structA = new TestSimpleBoxedA(42, (byte) 43, 42.5, TestEnum.VALUE3);
        var structB = new TestSimpleBoxedB((byte) 42, structA);
        assertEquals(42, structB.readSomeInt8());
        assertEquals(43, structB.readNestedA().readSomeInt8());
    }

    @Test
    void updateNested() {
        var structA = new TestSimpleBoxedA(42, (byte) 43, 42.5, TestEnum.VALUE3);
        var structB = new TestSimpleBoxedB((byte) 42, structA);
        assertEquals(42, structB.readNestedA().readSomeInt());

        var structA2 = new TestSimpleBoxedA(52, (byte) 43, 42.5, TestEnum.VALUE3);
        structB.writeNestedA(structA2);
        assertEquals(52, structB.readNestedA().readSomeInt());

        structB.readNestedA().writeSomeInt(53);
        assertEquals(53, structB.readNestedA().readSomeInt());

        // The nested struct was copied, so the originals are unchanged:
        assertEquals(52, structA2.readSomeInt());
        assertEquals(42, structA.readSomeInt());
    }

    @Test
    void createOpaque() {
        var struct1 = new TestBoxed();
        struct1.writeSomeInt8((byte) 42);
        assertEquals(42, struct1.readSomeInt8());

        var struct2 = TestBoxed.alternativeConstructor1((byte) 42);
        assertEquals(42, struct2.readSomeInt8());

        var struct3 = TestBoxed.alternativeConstructor2(40, 2);
        assertEquals(42, struct3.readSomeInt8());

        var struct4 = TestBoxed.alternativeConstructor3("42");
        assertEquals(42, struct4.readSomeInt8());
    }

    @Test
    void create() {
        var struct = new TestBoxedB((byte) 7, 5);
        assertEquals(7, struct.readSomeInt8());
        assertEquals(5, struct.readSomeLong());
    }

    @Test
    void createRefcounted() {
        var struct = new TestBoxedC();
        assertEquals(42, struct.readAnotherThing());
    }

    @Test
    void createPrivate() {
        var struct = new TestBoxedD("abcd", 8);
        assertEquals(12, struct.getMagic());
    }
}
