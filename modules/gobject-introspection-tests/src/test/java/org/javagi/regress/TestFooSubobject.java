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

import org.gnome.gi.regress.AnnotationObject;
import org.gnome.gi.regress.FooSubobject;
import org.gnome.gi.utility.Glyph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

public class TestFooSubobject {
    public static class MySubobject extends FooSubobject {
        public boolean virtualMethodCalled = false;
        public boolean readFnCalled = false;

        @Override
        public boolean virtualMethod(int firstParam) {
            this.virtualMethodCalled = true;
            return true;
        }

        /*
         * read_fn is a virtual method that is not supposed to be called
         */
        @SuppressWarnings("Unused")
        public void readFn(int offset, int length) {
            this.readFnCalled = true;
        }
    }

    MySubobject o;

    @BeforeEach
    void init() {
        o = new MySubobject();
    }

    @Test
    void externalType() {
        assertNull(o.externalType());
    }

    @Test
    void various() {
        o.various(null, AnnotationObject.getType());
    }

    @Test
    void isItTimeYet() {
        o.isItTimeYet(LocalTime.now().toEpochSecond(LocalDate.EPOCH, ZoneOffset.UTC));
    }

    @Test
    void seek() {
        o.seek(0x7fff_ffff_ffff_ffffL);
    }

    @Test
    void getName() {
        assertEquals("regress_foo", o.getName());
    }

    @Test
    void dupName() {
        assertEquals("regress_foo", o.dupName());
    }

    @Test
    void handleGlyph() {
        o.handleGlyph(new Glyph(0x2212));
    }

    @Test
    void appendNewStackLayer() {
        assertNull(o.appendNewStackLayer(5));
    }

    @Test
    void virtualMethod1() {
        o.doRegressFoo(777);
    }

    @Test
    void virtualMethod2() {
        assertTrue(o.virtualMethod(77));
        assertTrue(o.virtualMethodCalled);
    }

    @Test
    void virtualMethod3() {
        o.read(0xff, 10);
        assertFalse(o.readFnCalled);
    }

}
