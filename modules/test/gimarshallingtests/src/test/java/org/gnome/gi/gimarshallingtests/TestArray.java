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

package org.gnome.gi.gimarshallingtests;

import org.gnome.gobject.Value;
import org.javagi.base.Out;
import org.javagi.gobject.types.Types;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestArray {
    private final int[] TEST_INT_ARRAY = {-1, 0, 1, 2};
    private final int[] TEST_UCS4_ARRAY = {0x63, 0x6f, 0x6e, 0x73, 0x74, 0x20,
                                           0x2665, 0x20, 0x75, 0x74, 0x66, 0x38};

    @Test
    void return_() {
        int[] array = arrayReturn();
        assertArrayEquals(TEST_INT_ARRAY, array);
    }

    @Test
    void returnEtc() {
        var sum = new Out<Integer>();
        int[] array = arrayReturnEtc(9, 5, sum);
        assertEquals(14, sum.get());
        assertArrayEquals(new int[] {9, 0, 1, 5}, array);
    }

    @Test
    void returnUnaligned() {
        byte[] array = arrayReturnUnaligned();
        assertNotNull(array);
        assertEquals(32, array.length);
        assertArrayEquals(new byte[] {1, 2, 3, 4}, Arrays.copyOfRange(array, 0, 4));
        cleanupUnalignedBuffer();
    }

    @Test
    void in() {
        arrayIn(TEST_INT_ARRAY);
    }

    @Test
    void inLenBefore() {
        arrayInLenBefore(TEST_INT_ARRAY);
    }

    @Test
    void inLenZeroTerminated() {
        arrayInLenZeroTerminated(TEST_INT_ARRAY);
    }

    @Test
    void stringIn() {
        arrayStringIn(new String[] {"foo", "bar"});
    }

    @Test
    void uint8In() {
        arrayUint8In(new byte[] {'a', 'b', 'c', 'd'});
    }

    @Test
    void int64In() {
        arrayInt64In(new long[] {-1, 0, 1, 2});
    }

    @Test
    void uint64In() {
        arrayUint64In(new long[] {-1, 0, 1, 2});
    }

    @Test
    void unicharIn() {
        arrayUnicharIn(TEST_UCS4_ARRAY);
    }

    @Test
    void boolIn() {
        arrayBoolIn(new boolean[] {true, false, true, true});
    }

    @Test
    void structIn() {
        var struct1 = new BoxedStruct();
        var struct2 = new BoxedStruct();
        var struct3 = new BoxedStruct();
        struct1.writeLong(1);
        struct2.writeLong(2);
        struct3.writeLong(3);
        var structs = new BoxedStruct[] {struct1, struct2, struct3};
        arrayStructIn(structs);
    }

    @Test
    void structValueIn() {
        var struct1 = new BoxedStruct();
        var struct2 = new BoxedStruct();
        var struct3 = new BoxedStruct();
        struct1.writeLong(1);
        struct2.writeLong(2);
        struct3.writeLong(3);
        var structs = new BoxedStruct[] {struct1, struct2, struct3};
        arrayStructValueIn(structs);
    }

    @Test
    void simpleStructIn() {
        var struct1 = new SimpleStruct(1, (byte) 0);
        var struct2 = new SimpleStruct(2, (byte) 0);
        var struct3 = new SimpleStruct(3, (byte) 0);
        var structs = new SimpleStruct[] {struct1, struct2, struct3};
        arraySimpleStructIn(structs);
    }

    @Test
    void multiValueIn() {
        var keys = new String[] {"one", "two", "three"};
        Value value1 = new Value();
        Value value2 = new Value();
        Value value3 = new Value();
        value1.init(Types.INT);
        value2.init(Types.INT);
        value3.init(Types.INT);
        value1.setInt(1);
        value2.setInt(2);
        value3.setInt(3);
        var values = new Value[] {value1, value2, value3};
        multiArrayKeyValueIn(keys, values);
    }

    @Test
    void structTakeIn() {
        var struct1 = new BoxedStruct();
        var struct2 = new BoxedStruct();
        var struct3 = new BoxedStruct();
        struct1.writeLong(1);
        struct2.writeLong(2);
        struct3.writeLong(3);
        var structs = new BoxedStruct[] {struct1, struct2, struct3};
        arrayStructTakeIn(structs);
    }

    @Test
    void enumIn() {
        arrayEnumIn(new Enum[] {Enum.VALUE1, Enum.VALUE2, Enum.VALUE3});
    }

    @Test
    void flagsIn() {
        arrayFlagsIn(new Flags[] {Flags.VALUE1, Flags.VALUE2, Flags.VALUE3});
    }

    @Test
    void inGuint64Len() {
        arrayInGuint64Len(TEST_INT_ARRAY);
    }

    @Test
    void inGuint8Len() {
        arrayInGuint8Len(TEST_INT_ARRAY);
    }

    @Test
    void out() {
        var v = new Out<int[]>();
        arrayOut(v);
        assertArrayEquals(TEST_INT_ARRAY, v.get());
    }

    @Test
    void outUninitialized() {
        assertDoesNotThrow(() -> arrayOutUninitialized(null));
        var v = new Out<>(TEST_INT_ARRAY);
        assertThrows(NullPointerException.class, () -> arrayOutUninitialized(v));
    }

    @Test
    void outUnaligned() {
        var v = new Out<byte[]>();
        arrayOutUnaligned(v);
        byte[] array = v.get();
        assertNotNull(array);
        assertEquals(32, array.length);
        assertArrayEquals(new byte[] {1, 2, 3, 4}, Arrays.copyOfRange(array, 0, 4));
        cleanupUnalignedBuffer();
    }

    @Test
    void outEtc() {
        var array = new Out<int[]>();
        var sum = new Out<Integer>();
        arrayOutEtc(9, array, 5, sum);
        assertEquals(14, sum.get());
        assertArrayEquals(new int[] {9, 0, 1, 5}, array.get());
    }

    @Test
    void boolOut() {
        var array = new Out<boolean[]>();
        arrayBoolOut(array);
        assertArrayEquals(new boolean[] {true, false, true, true}, array.get());
    }

    @Test
    void unicharOut() {
        var array = new Out<int[]>();
        arrayUnicharOut(array);
        assertArrayEquals(TEST_UCS4_ARRAY, array.get());
    }

    @Test
    void inout() {
        var array = new Out<>(TEST_INT_ARRAY);
        arrayInout(array);
        assertArrayEquals(new int[] {-2, -1, 0, 1, 2}, array.get());
    }

    @Test
    void inoutEtc() {
        var array = new Out<>(TEST_INT_ARRAY);
        var sum = new Out<Integer>();
        arrayInoutEtc(9, array, 5, sum);
        assertEquals(14, sum.get());
        assertArrayEquals(new int[] {9, -1, 0, 1, 5}, array.get());
    }

    @Test
    void inNonzeroNonlen() {
        arrayInNonzeroNonlen(42, new byte[] {'a', 'b', 'c', 'd'});
    }
}
