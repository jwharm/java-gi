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
import org.gnome.glib.List;
import org.gnome.gobject.GObject;
import org.javagi.base.Out;
import org.javagi.base.TransferOwnership;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.util.Arrays;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestStruct {
    @Test
    void setFields() {
        var struct = new TestStructA(42, (byte) 43, 42.5, TestEnum.VALUE3);
        assertEquals(42, struct.readSomeInt());
        assertEquals((byte) 43, struct.readSomeInt8());
        assertEquals(42.5, struct.readSomeDouble());
        assertEquals(TestEnum.VALUE3, struct.readSomeEnum());
    }

    @Test
    void modifyWithMethod() {
        TestStructA c = new TestStructA();
        TestStructA.parse(c, "foobar");
        assertEquals(23, c.readSomeInt());
    }

    @Test
    void outArrayOfStructs() {
        var out = new Out<TestStructA[]>();
        testArrayStructOut(out);
        assertNotNull(out.get());
        var ints = Arrays.stream(out.get()).map(TestStructA::readSomeInt).toList();
        assertIterableEquals(ints, java.util.List.of(22, 33, 44));
    }

    @Test
    void nestedStruct() {
        var struct = new TestStructB((byte) 43, new TestStructA());
        struct.readNestedA().writeSomeInt8((byte) 66);
        assertEquals(43, struct.readSomeInt8());
        assertEquals(66, struct.readNestedA().readSomeInt8());
    }

    @Test
    void structWithGObject() {
        var struct = new TestStructC(43, new GObject());
        assertEquals(43, struct.readAnotherInt());
        assertInstanceOf(GObject.class, struct.readObj());
    }

    @Test
    void structWithArrays() {
        var structArray = new TestStructA[] {new TestStructA(), new TestStructA()};
        var objArray = new TestObj[] {new TestObj(), new TestObj(), new TestObj()};
        var objList = new List<>(TestObj::new, null, TransferOwnership.NONE);
        for (int i = 0; i < 4; i++)
            objList.add(new TestObj());
        var struct = new TestStructD(structArray, objArray, objArray[0], objList, objArray);

        assertEquals(2, struct.readArray1().length);
        assertEquals(3, struct.readArray2().length);
        assertInstanceOf(TestObj.class, struct.readField());
        assertEquals(4, struct.readList().size());
        assertEquals(3, struct.readGarray().length);
    }

    @Test
    @Disabled("Field with flat array of structs/unions is not supported by java-gi")
    void structWithUnions() {
        var type = GObject.getType();
        var unions = new TestStructESomeUnionUnion[] {
            new TestStructESomeUnionUnion(Arena.ofAuto()),
            new TestStructESomeUnionUnion(Arena.ofAuto())
        };
        new TestStructE(type, unions);
    }

    @Test
    void structWithConstAndVolatileMembers() {
        var struct = new TestStructF(1, null, 0, 0, null, 0, 0, (byte) 42);
        assertEquals(1, struct.readRefCount());
        assertEquals(42, struct.readData7());
    }
}
