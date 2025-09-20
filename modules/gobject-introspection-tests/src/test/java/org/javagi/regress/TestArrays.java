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

import org.gnome.gi.regress.TestObj;
import org.gnome.gio.Icon;
import org.gnome.gio.SimpleAction;
import org.gnome.glib.Type;
import org.javagi.base.Out;
import org.javagi.gobject.types.Types;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestArrays {
    @Test
    void intIn() {
        assertEquals(10, testArrayIntIn(new int[] {1, 2, 3, 4}));
        assertEquals(10, testArrayGint8In(new byte[] {1, 2, 3, 4}));
        assertEquals(10, testArrayGint16In(new short[] {1, 2, 3, 4}));
        assertEquals(10, testArrayGint32In(new int[] {1, 2, 3, 4}));
        assertEquals(10, testArrayGint64In(new long[] {1, 2, 3, 4}));
    }

    @Test
    void intOut() {
        var out = new Out<int[]>();
        testArrayIntOut(out);
        assertArrayEquals(new int[] {0, 1, 2, 3, 4}, out.get());
    }

    @Test
    void intInout() {
        var inout = new Out<>(new int[] {0, 1, 2, 3, 4});
        testArrayIntInout(inout);
        assertArrayEquals(new int[] {2, 3, 4, 5}, inout.get());
    }

    @Test
    void stringIn() {
        assertTrue(testStrvIn(new String[] {"1", "2", "3"}));
        assertFalse(testStrvIn(new String[] {"1", "2"}));
        assertFalse(testStrvIn(new String[] {"4", "5", "6"}));
        assertFalse(testStrvIn(new String[] {"1", "5", "6"}));
        assertFalse(testStrvIn(new String[] {"1", "2", "6"}));
        assertFalse(testStrvIn(new String[] {"4", "5", null}));
    }

    @Test
    void stringReturn() {
        assertArrayEquals(new String[] {"thanks", "for", "all", "the", "fish"}, testStrvOut());
    }

    @Test
    void stringReturnContainer() {
        assertArrayEquals(new String[] {"1", "2", "3"}, testStrvOutContainer());
    }

    @Test
    void stringOut() {
        var out = new Out<String[]>();
        testStrvOutarg(out);
        assertArrayEquals(new String[] {"1", "2", "3"}, out.get());
    }

    @Test
    void gtype() {
        var array = new Type[] {SimpleAction.getType(), Icon.getType(), Types.BOXED};
        assertEquals("[GSimpleAction,GIcon,GBoxed,]", testArrayGtypeIn(array));

        // Probably should make it throw a NullPointerException instead
        assertThrows(AssertionError.class, () -> testArrayGtypeIn(new Type[] {null}));
    }

    @Test
    void fixedSizeIntIn() {
        assertEquals(10, testArrayFixedSizeIntIn(new int[] {0, 1, 2, 3, 4}));
        assertThrows(IllegalArgumentException.class, () -> testArrayFixedSizeIntIn(new int[] {0, 1, 2, 3}));
        assertEquals(10, testArrayFixedSizeIntIn(new int[] {0, 1, 2, 3, 4, 5}));
    }

    @Test
    void fixedSizeIntOut() {
        var out = new Out<int[]>();
        testArrayFixedSizeIntOut(out);
        assertArrayEquals(new int[] {0, 1, 2, 3, 4}, out.get());
    }

    @Test
    void fixedSizeIntReturn() {
        assertArrayEquals(new int[] {0, 1, 2, 3, 4}, testArrayFixedSizeIntReturn());
    }

    @Test
    void staticLengthInt() {
        var array = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        testArrayStaticInInt(array);
    }

    @Test
    void constStrv() {
        var array = new String[] {"thanks", "for", "all", "the", "fish"};
        assertArrayEquals(array, testStrvOutC());
    }

    @Test
    void intFullOut() {
        assertArrayEquals(new int[] {0, 1, 2, 3, 4}, testArrayIntFullOut());
    }

    @Test
    void intNoneOut() {
        assertArrayEquals(new int[] {1, 2, 3, 4, 5}, testArrayIntNoneOut());
    }

    @Test
    void intNullIn() {
        testArrayIntNullIn(null);
    }

    @Test
    void fixedSizeObjectOut() {
        var objs = new Out<TestObj[]>();
        testArrayFixedOutObjects(objs);
        assertNotNull(objs.get());
        assertEquals(2, objs.get().length);
        assertInstanceOf(TestObj.class, objs.get()[0]);
        assertInstanceOf(TestObj.class, objs.get()[1]);
    }
}
