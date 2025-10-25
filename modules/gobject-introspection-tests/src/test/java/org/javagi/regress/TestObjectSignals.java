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
import org.gnome.gi.regress.TestSimpleBoxedA;
import org.gnome.gi.regress.TestSubObj;
import org.gnome.gio.Gio;
import org.gnome.gio.IOErrorEnum;
import org.gnome.glib.GError;
import org.gnome.glib.GLib;
import org.gnome.glib.HashTable;
import org.javagi.interop.Interop;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TestObjectSignals {
    @Test
    void connection() {
        var o = new TestObj();
        var counter = new AtomicInteger(0);
        var handler = o.onTest(counter::incrementAndGet);
        o.emitTest();
        assertEquals(1, counter.get());
        handler.disconnect();
        o.emitTest();
        assertEquals(1, counter.get());
    }

    @Test
    void invalid() {
        var o = new TestObj();
        assertThrows(IllegalArgumentException.class, () -> o.emit("invalid-signal"));
        // Calling o.connect("invalid-signal") doesn't throw, but logs a critical error.
    }

    @Test @Disabled("FIXME: boxed signal parameters should be passed by reference")
    void staticScopeArg() {
        var o = new TestObj();
        var b = new TestSimpleBoxedA();
        b.writeSomeInt(42);
        o.onTestWithStaticScopeArg(arg -> arg.writeSomeInt(44));
        o.emitTestWithStaticScopeArg(b);
        assertEquals(44, b.readSomeInt());
    }

    @Test
    void object() {
        var o = new TestObj();
        var counter = new AtomicInteger(0);
        o.onSigWithObj(obj -> {
            assertEquals(33, obj.getProperty("int"));
            counter.incrementAndGet();
        });
        var testObj = TestSubObj.builder().build();
        testObj.setProperty("int", 33);
        o.emit("sig-with-obj", testObj);
        assertEquals(1, counter.get());
    }

    @Test
    void objectFull() {
        var o = new TestObj();
        var counter = new AtomicInteger(0);
        o.onSigWithObjFull(obj -> {
            assertEquals(5, obj.getProperty("int"));
            counter.incrementAndGet();
        });
        o.emitSigWithObjFull(); // argument from native function
        assertEquals(1, counter.get());
    }

    @Test
    void objectFullWithArg() {
        var o = new TestObj();
        var counter = new AtomicInteger(0);
        o.onSigWithObjFull(obj -> {
            assertEquals(33, obj.getProperty("int"));
            counter.incrementAndGet();
        });
        var testObj = TestSubObj.builder().build();
        testObj.setProperty("int", 33);
        o.emit("sig-with-obj-full", testObj); // argument from java
        assertEquals(1, counter.get());
    }

    @Test @Disabled("Assert(ret == G_MAXINT64) fails, reason unknown")
    void int64() {
        var o = new TestObj();
        var counter = new AtomicInteger(0);
        o.onSigWithInt64Prop(number -> {
            assertEquals(GLib.MAXINT64, number);
            counter.incrementAndGet();
            return GLib.MAXINT64;
        });
        o.emitSigWithInt64();
        assertEquals(1, counter.get());
    }

    @Test @Disabled("Assert(ret == G_MAXUINT64) fails, reason unknown")
    void uint64() {
        var o = new TestObj();
        var counter = new AtomicInteger(0);
        o.onSigWithUint64Prop(number -> {
            assertEquals(GLib.MAXUINT64, number);
            counter.incrementAndGet();
            return GLib.MAXUINT64;
        });
        o.emitSigWithUint64();
        assertEquals(1, counter.get());
    }

    @Test @Disabled("Not yet implemented")
    void array() {
        var o = new TestObj();
        var counter = new AtomicInteger(0);
        o.onSigWithArrayProp(arr -> {
            assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5}, arr);
            counter.incrementAndGet();
        });
        var array = new int[] {0, 1, 2, 3, 4, 5};
        o.emit("sig-with-array-prop", (Object) array);
        assertEquals(1, counter.get());
    }

    @Test @Disabled("Not yet implemented")
    void arrayLen() {
        var o = new TestObj();
        var counter = new AtomicInteger(0);
        o.onSigWithArrayLenProp(arr -> {
            assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5}, arr);
            counter.incrementAndGet();
        });
        var array = new int[] {0, 1, 2, 3, 4, 5};
        o.emit("sig-with-array-len-prop", (Object) array);
        assertEquals(1, counter.get());
    }

    @Test
    void hash() {
        HashTable<String, Byte> hashTable1 = new HashTable<>(
                GLib::strHash,
                GLib::strEqual,
                Interop::getStringFrom,
                Interop::getByteFrom);
        hashTable1.putAll(Map.of("a", (byte) 1, "b", (byte) 2));

        var o = new TestObj();
        var counter = new AtomicInteger(0);
        o.onSigWithHashProp(hash -> {
            assertInstanceOf(HashTable.class, hash);
            counter.incrementAndGet();
        });
        o.emit("sig-with-hash-prop", hashTable1);
        assertEquals(1, counter.get());
    }

    @Test
    void strv() {
        var o = new TestObj();
        var counter = new AtomicInteger(0);
        o.onSigWithStrv(arr -> {
            assertArrayEquals(new String[] {"a", "bb", "ccc"}, arr);
            counter.incrementAndGet();
        });
        var array = new String[] {"a", "bb", "ccc"};
        o.emit("sig-with-strv", (Object) array);
        assertEquals(1, counter.get());
        o.emitSigWithStrv(array);
        assertEquals(2, counter.get());
    }

    @Test @Disabled("Not yet implemented")
    void strvFull() {
        var o = new TestObj();
        var counter = new AtomicInteger(0);
        o.onSigWithStrvFull(arr -> {
            assertArrayEquals(new String[] {"a", "bb", "ccc"}, arr);
            counter.incrementAndGet();
        });
        var array = new String[] {"a", "bb", "ccc"};
        o.emit("sig-with-strv-full", (Object) array);
        assertEquals(1, counter.get());
        o.emitSigWithStrvFull(array);
        assertEquals(2, counter.get());
    }

    @Test @Disabled("Not yet implemented")
    void intArrayRet() {
        var o = new TestObj();
        o.onSigWithIntarrayRet(a -> {
            int[] array = new int[a];
            for (int i = 0; i < a; i++)
                array[i] = i;
            return array;
        });
        int[] ret = o.emitSigWithIntarrayRet(5);
        assertArrayEquals(new int[] {0, 1, 2, 3, 4}, ret);
    }

    @Test
    void arrayLenProp() {
        var o = new TestObj();
        var counter = new AtomicInteger(0);
        o.onSigWithArrayLenProp(array -> {
            assertArrayEquals(new int[] {0, 1, 2, 3, 4}, array);
            counter.incrementAndGet();
        });
        // FIXME: we still have to allocate the array manually
        try (var arena = Arena.ofConfined()) {
            var data = Interop.allocateNativeArray(new int[] {0, 1, 2, 3, 4}, false, arena);
            o.emit("sig-with-array-len-prop", data, 5);
        }
        assertEquals(1, counter.get());
        o.emitSigWithArrayLenProp();
        assertEquals(2, counter.get());
    }

    @Test
    void inoutInt() {
        var o = new TestObj();
        o.onSigWithInoutInt(position -> {
            assertEquals(42, position.get());
            position.set(43);
        });
        // FIXME: we still have to allocate the array manually
        try (var arena = Arena.ofConfined()) {
            var value = arena.allocateFrom(ValueLayout.JAVA_INT, 42);
            o.emit("sig-with-inout-int", value);
            assertEquals(43, value.get(ValueLayout.JAVA_INT, 0));

        }
        o.emitSigWithInoutInt();
    }

    @Test @Disabled("Not yet implemented")
    void gerror() {
        var o = new TestObj();
        o.onSigWithGerror(err -> {
            assertNotNull(err);
            // This only works when we manually dereference the pointer like this:
                var err2 = new GError(Interop.dereference(err.handle()));
                assertEquals(Gio.ioErrorQuark(), err2.readDomain());
                assertEquals(IOErrorEnum.FAILED.getValue(), err2.readCode());
            // I'm not sure why this is necessary here, so I disabled the test for now.
        });
        o.emitSigWithError();
    }

    @Test
    void gerrorNull() {
        var o = new TestObj();
        o.onSigWithGerror(err -> {
            assertNotNull(err);
            assertThrows(NullPointerException.class, () -> Interop.checkNull(err.handle()));
        });
        o.emitSigWithNullError();
        o.emit("sig-with-gerror", (Object) null);
    }
}
