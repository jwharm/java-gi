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

import org.gnome.gi.gimarshallingtests.Enum;
import org.gnome.gi.gimarshallingtests.Flags;
import org.gnome.gi.gimarshallingtests.GIMarshallingTestsObject;
import org.gnome.gio.Gio;
import org.gnome.gio.IOErrorEnum;
import org.gnome.glib.GLib;
import org.gnome.glib.SpawnError;
import org.gnome.glib.Type;
import org.gnome.gobject.GObject;
import org.gnome.gobject.Value;
import org.javagi.base.GErrorException;
import org.javagi.base.Out;
import org.javagi.gobject.types.TypeCache;
import org.javagi.gobject.types.Types;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestObjectVirtualMethod {
    static class VFuncTester extends GIMarshallingTestsObject {
        @Override
        public void methodInt8In(byte in) {
            setProperty("int", (int) in);
        }

        @Override
        public void methodInt8Out(Out<Byte> out) {
            out.set((byte) 40);
        }

        @Override
        public void methodInt8ArgAndOutCaller(byte arg, Out<Byte> out) {
            out.set((byte) (arg + 3));
        }

        @Override
        public void methodInt8ArgAndOutCallee(byte arg, Out<Byte> out) {
            out.set((byte) (arg + 4));
        }

        @Override
        public String methodStrArgOutRet(String arg, Out<Integer> out) {
            out.set(41);
            return "Called with " + arg;
        }

        @Override
        public void methodWithDefaultImplementation(byte in) {
            setProperty("int", in + 2);
        }

        @Override
        public int vfuncReturnValueOnly() {
            return 42;
        }

        @Override
        public void vfuncOneOutParameter(Out<Float> a) {
            a.set(43f);
        }

        @Override
        public void vfuncMultipleOutParameters(Out<Float> a, Out<Float> b) {
            a.set(44f);
            b.set(45f);
        }

        @Override
        public int vfuncReturnValueAndOneOutParameter(Out<Integer> a) {
            a.set(46);
            return 47;
        }

        @Override
        public int vfuncReturnValueAndMultipleOutParameters(Out<Integer> a, Out<Integer> b) {
            a.set(48);
            b.set(49);
            return 50;
        }

        @Override
        public void vfuncArrayOutParameter(Out<float[]> a) {
            a.set(new float[] {50f, 51f});
        }

        @Override
        public void vfuncCallerAllocatedOutParameter(Value a) {
            a.setInt(52);
        }

        @Override
        public boolean vfuncMethWithError(int x) throws GErrorException {
            return switch (x) {
                case 1 -> true;
                case 2 -> throw new GErrorException(Gio.ioErrorQuark(), IOErrorEnum.FAILED.getValue(), "I FAILED, but the test passed!");
                case 3 -> throw new GErrorException(GLib.spawnErrorQuark(), SpawnError.TOO_BIG.getValue(), "This test is Too Big to Fail");
                case 4 -> throw new NullPointerException();
                default -> false;
            };
        }

        @Override
        public Enum vfuncReturnEnum() {
            return Enum.VALUE2;
        }

        @Override
        public void vfuncOutEnum(Out<Enum> _enum) {
            _enum.set(Enum.VALUE3);
        }

        @Override
        public Set<Flags> vfuncReturnFlags() {
            return Set.of(Flags.VALUE2);
        }

        @Override
        public void vfuncOutFlags(Out<Set<Flags>> flags) {
            flags.set(Set.of(Flags.VALUE3));
        }

        @Override
        protected GObject vfuncReturnObjectTransferNone() {
            return new GIMarshallingTestsObject(53);
        }

        @Override
        protected GObject vfuncReturnObjectTransferFull() {
            return new GIMarshallingTestsObject(54);
        }

        @Override
        protected void vfuncOutObjectTransferNone(Out<GObject> object) {
            object.set(new GIMarshallingTestsObject(55));
        }

        @Override
        protected void vfuncOutObjectTransferFull(Out<GObject> object) {
            object.set(new GIMarshallingTestsObject(56));
        }

        @Override
        protected void vfuncInObjectTransferNone(GObject object) {
        }

        public GObject inObject;

        @Override
        protected void vfuncInObjectTransferFull(GObject object) {
            this.inObject = object;
        }

        @Override
        public void vfuncOneInoutParameter(Out<Float> a) {
            a.set(a.get() * 5);
        }

        @Override
        public void vfuncMultipleInoutParameters(Out<Float> a, Out<Float> b) {
            a.set(a.get() * 5);
            b.set(b.get() * -1);
        }

        @Override
        public int vfuncReturnValueAndOneInoutParameter(Out<Integer> a) {
            a.set(a.get() * 5);
            return 49;
        }

        @Override
        public int vfuncReturnValueAndMultipleInoutParameters(Out<Integer> a, Out<Integer> b) {
            a.set(a.get() * 5);
            b.set(b.get() * -1);
            return 49;
        }
    }

    VFuncTester tester;

    @BeforeEach
    void constructTester() {
        tester = new VFuncTester();
    }

    @Test
    void methodInt8In() {
        tester.methodInt8In((byte) 39);
        assertEquals(39, tester.getProperty("int"));
    }

    @Test
    void int8In() {
        tester.int8In((byte) 39);
        assertEquals(39, tester.getProperty("int"));
    }

    @Test
    void methodInt8Out() {
        var v = new Out<Byte>();
        tester.methodInt8Out(v);
        assertEquals((byte) 40, v.get());
    }

    @Test
    void int8Out() {
        var v = new Out<Byte>();
        tester.int8Out(v);
        assertEquals((byte) 40, v.get());
    }

    @Test
    void methodInt8ArgAndOutCaller() {
        var v = new Out<Byte>();
        tester.methodInt8ArgAndOutCaller((byte) 39, v);
        assertEquals((byte) 42, v.get());
    }

    @Test
    void methodInt8ArgAndOutCallee() {
        var v = new Out<Byte>();
        tester.methodInt8ArgAndOutCallee((byte) 38, v);
        assertEquals((byte) 42, v.get());
    }

    @Test
    void methodStrArgOutRet() {
        var v = new Out<Integer>();
        assertEquals("Called with a string", tester.methodStrArgOutRet("a string", v));
        assertEquals(41, v.get());
        assertEquals("Called with a 2nd string", tester.methodStrArgOutRet("a 2nd string", v));
        assertEquals(41, v.get());
    }

    @Test
    void methodWithDefaultImplementation() {
        tester.methodWithDefaultImplementation((byte) 40);
        assertEquals(42, tester.getProperty("int"));
    }

    @Test
    void vfuncReturnValueOnly() {
        assertEquals(42, tester.vfuncReturnValueOnly());
    }

    @Test
    void vfuncOneOutParameter() {
        var v = new Out<Float>();
        tester.vfuncOneOutParameter(v);
        assertEquals(43, v.get());
    }

    @Test
    void vfuncMultipleOutParameters() {
        var v1 = new Out<Float>();
        var v2 = new Out<Float>();
        tester.vfuncMultipleOutParameters(v1, v2);
        assertEquals(44, v1.get());
        assertEquals(45, v2.get());
    }

    @Test
    void vfuncReturnValueAndOneOutParameter() {
        var v = new Out<Integer>();
        int ret = tester.vfuncReturnValueAndOneOutParameter(v);
        assertEquals(46, v.get());
        assertEquals(47, ret);
    }

    @Test
    void vfuncReturnValueAndMultipleOutParameters() {
        var v1 = new Out<Integer>();
        var v2 = new Out<Integer>();
        int ret = tester.vfuncReturnValueAndMultipleOutParameters(v1, v2);
        assertEquals(48, v1.get());
        assertEquals(49, v2.get());
        assertEquals(50, ret);
    }

    @Test
    void vfuncOneInoutParameter() {
        var v = new Out<>(10f);
        tester.vfuncOneInoutParameter(v);
        assertEquals(50, v.get());
    }

    @Test
    void vfuncMultipleInoutParameters() {
        var v1 = new Out<>(10f);
        var v2 = new Out<>(5f);
        tester.vfuncMultipleInoutParameters(v1, v2);
        assertEquals(50, v1.get());
        assertEquals(-5, v2.get());
    }

    @Test
    void vfuncReturnValueAndOneInoutParameter() {
        var v = new Out<>(10);
        int ret = tester.vfuncReturnValueAndOneInoutParameter(v);
        assertEquals(49, ret);
        assertEquals(50, v.get());
    }

    @Test
    void vfuncReturnValueAndMultipleInoutParameters() {
        var v1 = new Out<>(10);
        var v2 = new Out<>(-51);
        int ret = tester.vfuncReturnValueAndMultipleInoutParameters(v1, v2);
        assertEquals(49, ret);
        assertEquals(50, v1.get());
        assertEquals(51, v2.get());
    }

    @Test
    void vfuncArrayOutParameter() {
        var v = new Out<float[]>();
        tester.vfuncArrayOutParameter(v);
        assertArrayEquals(new float[] {50, 51}, v.get());
    }

    @Test
    void vfuncCallerAllocatedOutParameter() {
        var value = new Value();
        value.init(Types.INT);
        tester.vfuncCallerAllocatedOutParameter(value);
        assertEquals(52, value.getInt());
    }

    @Test
    void vfuncMethWithError() {
        assertDoesNotThrow(() -> assertTrue(tester.vfuncMethWithError(1)));
        assertThrows(NullPointerException.class, () -> tester.vfuncMethWithError(4));
        assertDoesNotThrow(() -> assertFalse(tester.vfuncMethWithError(5)));

        try {
            tester.vfuncMethWithError(2);
            fail();
        } catch(GErrorException e) {
            assertEquals(Gio.ioErrorQuark(), e.getDomain());
            assertEquals(IOErrorEnum.FAILED.getValue(), e.getCode());
            assertEquals("I FAILED, but the test passed!", e.getMessage());
        }

        try {
            tester.vfuncMethWithError(3);
            fail();
        } catch(GErrorException e) {
            assertEquals(GLib.spawnErrorQuark(), e.getDomain());
            assertEquals(SpawnError.TOO_BIG.getValue(), e.getCode());
            assertEquals("This test is Too Big to Fail", e.getMessage());
        }
    }

    @Test
    void vfuncReturnEnum() {
        assertEquals(Enum.VALUE2, tester.vfuncReturnEnum());
    }

    @Test
    void vfuncOutEnum() {
        var v = new Out<Enum>();
        tester.vfuncOutEnum(v);
        assertEquals(Enum.VALUE3, v.get());
    }

    @Test
    void vfuncReturnFlags() {
        assertEquals(Set.of(Flags.VALUE2), tester.vfuncReturnFlags());
    }

    @Test
    void vfuncOutFlags() {
        var v = new Out<Set<Flags>>();
        tester.vfuncOutFlags(v);
        assertEquals(Set.of(Flags.VALUE3), v.get());
    }

    @Test
    void vfuncReturnObjectTransferNone() {
        var refCount = new Out<Integer>();
        var isFloating = new Out<Boolean>();
        tester.getRefInfoForVfuncReturnObjectTransferNone(refCount, isFloating);
        assertEquals(1, refCount.get());
        assertFalse(isFloating.get());
    }

    @Test
    void vfuncReturnObjectTransferFull() {
        var refCount = new Out<Integer>();
        var isFloating = new Out<Boolean>();
        tester.getRefInfoForVfuncReturnObjectTransferFull(refCount, isFloating);
        assertEquals(2, refCount.get());
        assertFalse(isFloating.get());
    }

    @Test
    void vfuncOutObjectTransferNone() {
        var refCount = new Out<Integer>();
        var isFloating = new Out<Boolean>();
        tester.getRefInfoForVfuncOutObjectTransferNone(refCount, isFloating);
        assertEquals(1, refCount.get());
        assertFalse(isFloating.get());
    }

    @Test
    void vfuncOutObjectTransferFull() {
        var refCount = new Out<Integer>();
        var isFloating = new Out<Boolean>();
        tester.getRefInfoForVfuncOutObjectTransferFull(refCount, isFloating);
        assertEquals(2, refCount.get());
        assertFalse(isFloating.get());
    }

    @Test @Disabled
    void vfuncInObjectTransferNone() {
        Type gtype = TypeCache.getType(VFuncTester.class);
        var refCount = new Out<Integer>();
        var isFloating = new Out<Boolean>();
        tester.getRefInfoForVfuncInObjectTransferNone(gtype, refCount, isFloating);
        assertEquals(2, refCount.get());
        assertFalse(isFloating.get());
    }

    @Test
    void vfuncInObjectTransferFull() {
        Type gtype = TypeCache.getType(VFuncTester.class);
        var refCount = new Out<Integer>();
        var isFloating = new Out<Boolean>();
        tester.getRefInfoForVfuncInObjectTransferFull(gtype, refCount, isFloating);
        assertEquals(1, refCount.get());
        assertFalse(isFloating.get());
    }
}
