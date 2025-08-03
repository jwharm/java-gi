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

import org.gnome.gi.gimarshallingtests.GIMarshallingTestsObject;
import org.javagi.base.GErrorException;
import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestObject {
    @Test
    void staticMethod() {
        GIMarshallingTestsObject.staticMethod();
    }

    @Test
    void method() {
        new GIMarshallingTestsObject(42).method();
    }

    @Test
    void overriddenMethod() {
        new GIMarshallingTestsObject(0).overriddenMethod();
    }

    @Test
    void constructor() {
        var o = new GIMarshallingTestsObject(42);
        assertEquals(42, o.getProperty("int"));
    }

    @Test
    void constructorThatFails() {
        assertThrows(GErrorException.class, () -> GIMarshallingTestsObject.fail(42));
    }

    @Test
    void noneReturn() {
        var o = GIMarshallingTestsObject.noneReturn();
        assertNotNull(o);
        assertEquals(0, o.getProperty("int"));
    }

    @Test
    void fullReturn() {
        var o = GIMarshallingTestsObject.fullReturn();
        assertNotNull(o);
        assertEquals(0, o.getProperty("int"));
    }

    @Test
    void noneOut() {
        var v = new Out<GIMarshallingTestsObject>();
        GIMarshallingTestsObject.noneOut(v);
        assertNotNull(v.get());
        assertEquals(0, v.get().getProperty("int"));
    }

    @Test
    void fullOut() {
        var v = new Out<GIMarshallingTestsObject>();
        GIMarshallingTestsObject.fullOut(v);
        assertNotNull(v.get());
        assertEquals(0, v.get().getProperty("int"));
    }

    @Test
    void noneOutUninitialized() {
        var v = new Out<GIMarshallingTestsObject>();
        GIMarshallingTestsObject.noneOutUninitialized(v);
        assertNull(v.get());
    }

    @Test
    void fullOutUninitialized() {
        var v = new Out<GIMarshallingTestsObject>();
        GIMarshallingTestsObject.fullOutUninitialized(v);
        assertNull(v.get());
    }

    @Test
    void noneInout() {
        var in = new GIMarshallingTestsObject(42);
        var v = new Out<>(in);
        GIMarshallingTestsObject.noneInout(v);
        var out = v.get();
        assertEquals(0, out.getProperty("int"));
    }

    @Test
    void fullInout() {
        var in = new GIMarshallingTestsObject(42);
        var v = new Out<>(in);
        GIMarshallingTestsObject.fullInout(v);
        var out = v.get();
        assertEquals(0, out.getProperty("int"));
    }

    @Test
    void noneIn() {
        var o = new GIMarshallingTestsObject(42);
        o.noneIn();
    }
}
