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
import org.gnome.gobject.GObject;
import org.gnome.gobject.Value;
import org.javagi.base.Out;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestObjectVirtualMethodWrong {
    static class WrongVFuncTester extends GIMarshallingTestsObject {
        @Override
        public int vfuncReturnValueOnly() {
            return 0;
        }

        @Override
        public void vfuncOneOutParameter(Out<Float> a) {
        }

        @Override
        public void vfuncMultipleOutParameters(Out<Float> a, Out<Float> b) {
        }

        @Override
        public int vfuncReturnValueAndOneOutParameter(Out<Integer> a) {
            return 0;
        }

        @Override
        public int vfuncReturnValueAndMultipleOutParameters(Out<Integer> a, Out<Integer> b) {
            return 0;
        }

        @Override
        public void vfuncArrayOutParameter(Out<float[]> a) {
        }

        @Override
        public void vfuncCallerAllocatedOutParameter(Value a) {
        }

        @Override
        public Enum vfuncReturnEnum() {
            return null;
        }

        @Override
        public void vfuncOutEnum(Out<Enum> _enum) {
        }

        @Override
        public Set<Flags> vfuncReturnFlags() {
            return null;
        }

        @Override
        public void vfuncOutFlags(Out<Set<Flags>> flags) {
        }

        @Override
        protected GObject vfuncReturnObjectTransferNone() {
            return null;
        }

        @Override
        protected GObject vfuncReturnObjectTransferFull() {
            return null;
        }

        @Override
        protected void vfuncOutObjectTransferNone(Out<GObject> object) {
        }

        @Override
        protected void vfuncOutObjectTransferFull(Out<GObject> object) {
        }

        @Override
        protected void vfuncInObjectTransferNone(GObject object) {
        }

        @Override
        protected void vfuncInObjectTransferFull(GObject object) {
        }
    }

    WrongVFuncTester tester;

    @BeforeEach
    void constructTester() {
        tester = new WrongVFuncTester();
    }

    @Test
    void vfuncReturnValueOnly() {
        assertEquals(0, tester.vfuncReturnValueOnly());
    }

    @Test
    void vfuncOneOutParameter() {
        var v = new Out<Float>();
        tester.vfuncOneOutParameter(v);
        assertNull(v.get());
    }

    @Test
    void vfuncMultipleOutParameters() {
        var v1 = new Out<Float>();
        var v2 = new Out<Float>();
        tester.vfuncMultipleOutParameters(v1, v2);
        assertNull(v1.get());
        assertNull(v2.get());
    }

    @Test
    void vfuncReturnValueAndOneOutParameter() {
        var v = new Out<Integer>();
        int ret = tester.vfuncReturnValueAndOneOutParameter(v);
        assertNull(v.get());
        assertEquals(0, ret);
    }

    @Test
    void vfuncReturnValueAndMultipleOutParameters() {
        var v1 = new Out<Integer>();
        var v2 = new Out<Integer>();
        int ret = tester.vfuncReturnValueAndMultipleOutParameters(v1, v2);
        assertNull(v1.get());
        assertNull(v2.get());
        assertEquals(0, ret);
    }

    @Test
    void vfuncArrayOutParameter() {
        var v = new Out<float[]>();
        tester.vfuncArrayOutParameter(v);
        assertNull(v.get());
    }

    @Test
    void vfuncReturnEnum() {
        assertNull(tester.vfuncReturnEnum());
    }

    @Test
    void vfuncOutEnum() {
        var v = new Out<Enum>();
        tester.vfuncOutEnum(v);
        assertNull(v.get());
    }

    @Test
    void vfuncReturnFlags() {
        assertNull(tester.vfuncReturnFlags());
    }

    @Test
    void vfuncOutFlags() {
        var v = new Out<Set<Flags>>();
        tester.vfuncOutFlags(v);
        assertNull(v.get());
    }
}
