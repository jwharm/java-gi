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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestArrayStruct {
    @Test
    void outNone() {
        var arr = new Out<TestStructA[]>();
        testArrayStructOutNone(arr);
        assertNotNull(arr.get());
        List<Integer> result = Arrays.stream(arr.get()).map(TestStructA::readSomeInt).toList();
        assertIterableEquals(List.of(111, 222, 333), result);
    }

    @Test
    void outContainer() {
        var arr = new Out<TestStructA[]>();
        testArrayStructOutContainer(arr);
        assertNotNull(arr.get());
        List<Integer> result = Arrays.stream(arr.get()).map(TestStructA::readSomeInt).toList();
        assertIterableEquals(List.of(11, 13, 17, 19, 23), result);
    }

    @Test
    void outFull() {
        var arr = new Out<TestStructA[]>();
        testArrayStructOutFullFixed(arr);
        assertNotNull(arr.get());
        List<Integer> result = Arrays.stream(arr.get()).map(TestStructA::readSomeInt).toList();
        assertIterableEquals(List.of(2, 3, 5, 7), result);
    }

    @Test @Disabled("Not supported")
    void outCallerAllocated() {
        // With caller-allocated array in, there's no way to supply the
        // length. This happens in GLib.MainContext.query()
        var arr = new Out<TestStructA[]>();
        testArrayStructOutCallerAlloc(arr);
        assertNotNull(arr.get());
        assertEquals(0, arr.get().length);
    }

    @Test
    void inFull() {
        TestStructA[] array = new TestStructA[] {
                new TestStructA(201, (byte) 0, 0.0, TestEnum.of(0)),
                new TestStructA(202, (byte) 0, 0.0, TestEnum.of(0))
        };
        testArrayStructInFull(array);
    }

    @Test
    void inNone() {
        TestStructA[] array = new TestStructA[] {
                new TestStructA(301, (byte) 0, 0.0, TestEnum.of(0)),
                new TestStructA(302, (byte) 0, 0.0, TestEnum.of(0)),
                new TestStructA(303, (byte) 0, 0.0, TestEnum.of(0))
        };
        testArrayStructInNone(array);
    }
}
