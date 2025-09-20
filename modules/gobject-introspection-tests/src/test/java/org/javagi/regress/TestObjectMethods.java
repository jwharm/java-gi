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
import org.gnome.gobject.GObject;
import org.javagi.base.GErrorException;
import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestObjectMethods {
    @Test
    void instanceMethod() {
        var o = new TestObj();
        assertEquals(-1, o.instanceMethod());
    }

    @Test
    void instanceMethodFull() {
        var o = new TestObj();
        o.instanceMethodFull();
    }

    @Test
    void staticMethod() {
        assertEquals(5, TestObj.staticMethod(5));
    }

    @Test
    void forcedMethod() {
        var o = new TestObj();
        o.forcedMethod();
    }

    @Test
    void tortureSignature0() {
        var o = new TestObj();
        var y = new Out<Double>();
        var z = new Out<Integer>();
        var q = new Out<Integer>();
        o.tortureSignature0(42, y, z, "foo", q, 7);
        assertEquals(42, y.get(), 0.01);
        assertEquals(84, z.get());
        assertEquals(10, q.get());
    }

    @Test
    void tortureSignature1Fail() {
        var o = new TestObj();
        var y = new Out<Double>();
        var z = new Out<Integer>();
        var q = new Out<Integer>();
        assertThrows(GErrorException.class, () -> o.tortureSignature1(42, y, z, "foo", q, 7));
    }

    @Test
    void tortureSignature1Success() {
        var o = new TestObj();
        var y = new Out<Double>();
        var z = new Out<Integer>();
        var q = new Out<Integer>();
        try {
            o.tortureSignature1(11, y, z, "barbaz", q, 8);
        } catch (GErrorException e) {
            fail();
        }
        assertEquals(11, y.get(), 0.01);
        assertEquals(22, z.get());
        assertEquals(14, q.get());
    }

    // Java-GI does not skip return values or parameters when annotated with (skip).
    @Test
    void doesNotSkipReturnVal() {
        var o = new TestObj();
        try {
            boolean result = o.skipReturnVal(0, null, 0.0, null, null, 0, 0);
            assertTrue(result);
        } catch (GErrorException e) {
            fail();
        }
    }

    // Java-GI does not skip return values or parameters when annotated with (skip).
    @Test
    void doesNotSkipParam() {
        var o = new TestObj();
        var b = new Out<Integer>();
        var d = new Out<>(2);
        var sum = new Out<Integer>();
        try {
            boolean result = o.skipParam(1, b, 1.5, d, sum, 3, 4);
            assertTrue(result);
        } catch (GErrorException e) {
            fail();
        }
        assertEquals(2, b.get());
        assertEquals(3, d.get());
        assertEquals(43, sum.get());
    }

    // Java-GI does not skip return values or parameters when annotated with (skip).
    @Test
    void doesNotSkipOutParam() {
        var o = new TestObj();
        var b = new Out<Integer>();
        var d = new Out<>(2);
        var sum = new Out<Integer>();
        try {
            boolean result = o.skipOutParam(1, b, 1.5, d, sum, 3, 4);
            assertTrue(result);
        } catch (GErrorException e) {
            fail();
        }
        assertEquals(2, b.get());
        assertEquals(3, d.get());
        assertEquals(43, sum.get());
    }

    // Java-GI does not skip return values or parameters when annotated with (skip).
    @Test
    void doesNotSkipInoutParam() {
        var o = new TestObj();
        var b = new Out<Integer>();
        var d = new Out<>(2);
        var sum = new Out<Integer>();
        try {
            boolean result = o.skipInoutParam(1, b, 1.5, d, sum, 3, 4);
            assertTrue(result);
        } catch (GErrorException e) {
            fail();
        }
        assertEquals(2, b.get());
        assertEquals(3, d.get());
        assertEquals(43, sum.get());
    }

    @Test
    void staticMethodHasOneArgument() {
        try {
            assertNotNull(TestObj.fromFile(""));
        } catch (GErrorException e) {
            fail();
        }
    }

    @Test
    void skipsDestroyNotifyAndUserDataArguments() {
        TestObj.callback(() -> 0);
    }

    @Test
    void virtualFunction() {
        var o = new TestObj();
        assertEquals(42, o.doMatrix("meaningless string"));
    }

    @Test
    void nullObjectIn() {
        funcObjNullIn(null);
        funcObjNullableIn(null);
    }

    @Test
    void nullObjectOut() {
        var out = new Out<TestObj>();
        TestObj.nullOut(out);
        assertNull(out.get());
    }

    @Test
    void gpointerWithTypeAnnotationIn() {
        var o2 = new GObject();
        new TestObj().notNullableTypedGpointerIn(o2);
    }

    @Test
    void gpointerWithElementTypeAnnotationIn() {
        var bytes = new byte[] {1, 2};
        new TestObj().notNullableElementTypedGpointerIn(bytes);
    }

    @Test
    void nameConflictWithProperty() {
        // Ensure that the "name-conflict" property can be set with the builder
        var o = TestObj.builder()
                .setNameConflict(42)
                .build();
        assertEquals(42, o.getProperty("name-conflict"));

        // Ensure that the "void nameConflict()" method is present on the class
        try {
            Method m = TestObj.class.getDeclaredMethod("nameConflict");
            assertEquals(Void.TYPE, m.getReturnType());
        } catch (NoSuchMethodException e) {
            fail(e);
        }
    }
}
