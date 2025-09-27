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
import org.gnome.gio.AsyncResult;
import org.gnome.gio.Cancellable;
import org.gnome.gio.Gio;
import org.gnome.gio.IOErrorEnum;
import org.gnome.glib.GLib;
import org.gnome.glib.HashTable;
import org.gnome.glib.MainLoop;
import org.javagi.base.GErrorException;
import org.javagi.interop.Interop;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestCallback {
    @Test
    void callback() {
        assertEquals(42, testCallback(() -> 42));
    }

    @Test
    void nullCallback() {
        assertEquals(0, testCallback(null));
        assertEquals(0, testMultiCallback(null));

        // This segfaults in native code
        // assertEquals(0, testArrayCallback(null));
    }

    @Test
    void moreThanOnce() {
        var count = new AtomicInteger(0);
        assertEquals(42, testMultiCallback(() -> {
            count.incrementAndGet();
            return 21;
        }));
        assertEquals(2, count.get());
    }

    @Test
    void arrayCallback() {
        int result = testArrayCallback((ints, strs) -> {
            assertArrayEquals(new int[] {-1, 0, 1, 2}, ints);
            assertArrayEquals(new String[] {"one", "two", "three"}, strs);
            return 7;
        });
        assertEquals(14, result);
    }

    @Test
    void simpleCallback() {
        var hasBeenCalled = new AtomicBoolean(false);
        testSimpleCallback(() -> hasBeenCalled.set(true));
        assertTrue(hasBeenCalled.get());

        assertDoesNotThrow(() -> testSimpleCallback(null));
    }

    @Test
    void noptrCallback() {
        var hasBeenCalled = new AtomicBoolean(false);
        testNoptrCallback(() -> hasBeenCalled.set(true));
        assertTrue(hasBeenCalled.get());

        assertDoesNotThrow(() -> testNoptrCallback(null));
    }

    @Test
    void introspectedFunctionAsParameter() {
        var expected = GLib.getNumProcessors();
        assertEquals(expected, testCallback(GLib::getNumProcessors));
    }

    @Test
    void userData() {
        assertEquals(7, testCallbackUserData(() -> 7));
    }

    @Test
    void returnFull() {
        var hasBeenCalled = new AtomicBoolean(false);
        testCallbackReturnFull(() -> {
                hasBeenCalled.set(true);
                return new TestObj();
        });
        assertTrue(hasBeenCalled.get());
    }

    @Test
    void destroyNotify() {
        var count1 = new AtomicInteger(0);
        var count2 = new AtomicInteger(0);
        assertEquals(42, testCallbackDestroyNotify(() -> {
            count1.incrementAndGet();
            return 42;
        }));
        assertEquals(1, count1.get());
        assertEquals(58, testCallbackDestroyNotify(() -> {
            count2.incrementAndGet();
            return 58;
        }));
        assertEquals(1, count2.get());
        assertEquals(100, testCallbackThawNotifications());
        assertEquals(2, count1.get());
        assertEquals(2, count2.get());
    }

    @Test
    void destroyNotifyNoUserData() {
        var count1 = new AtomicInteger(0);
        var count2 = new AtomicInteger(0);
        assertEquals(42, testCallbackDestroyNotifyNoUserData(() -> {
            count1.incrementAndGet();
            return 42;
        }));
        assertEquals(1, count1.get());
        assertEquals(58, testCallbackDestroyNotifyNoUserData(() -> {
            count2.incrementAndGet();
            return 58;
        }));
        assertEquals(1, count2.get());
        assertEquals(100, testCallbackThawNotifications());
        assertEquals(2, count1.get());
        assertEquals(2, count2.get());
    }

    @Test
    void async() {
        testCallbackAsync(() -> 44);
        assertEquals(44, testCallbackThawAsync());
    }

    /**
     * Run a GLib event loop that will handle all pending events and then quit.
     */
    private void runMainLoopOnce() {
        var mainLoop = new MainLoop(null, false);
        GLib.idleAddOnce(mainLoop::quit);
        mainLoop.run();
    }

    @Test
    void asyncReady() {
        var hasBeenCalled = new AtomicBoolean(false);
        testAsyncReadyCallback((obj, res, _) -> {
            hasBeenCalled.set(true);
            assertNull(obj);
            assertInstanceOf(AsyncResult.class, res);
        });
        runMainLoopOnce();
        assertTrue(hasBeenCalled.get());
    }

    @Test
    void instanceMethod() {
        var count = new AtomicInteger(0);
        var o = new TestObj();
        org.gnome.gi.regress.TestCallback callback = count::incrementAndGet;
        o.instanceMethodCallback(callback);
        assertEquals(1, count.get());
        o.instanceMethodCallback(null);
        assertEquals(1, count.get());
    }

    @Test
    void asyncInstanceMethod() {
        var o = new TestObj();
        int prio = GLib.PRIORITY_DEFAULT;
        var cancel = new Cancellable();
        assertTrue(o.functionSync(prio));

        var hasBeenCalled = new AtomicBoolean(false);
        o.functionAsync(prio, cancel, (obj, res, _) -> {
            assertSame(o, obj);
            try {
                hasBeenCalled.set(true);
                assertTrue(o.functionFinish(res));
            } catch (GErrorException e) {
                fail(e);
            }
        });
        assertEquals(1, o.functionThawAsync());
        runMainLoopOnce();
        assertTrue(hasBeenCalled.get());
    }

    @Test
    void staticMethod() {
        var count = new AtomicInteger(0);
        org.gnome.gi.regress.TestCallback callback = count::incrementAndGet;
        TestObj.staticMethodCallback(callback);
        assertEquals(1, count.get());
        TestObj.staticMethodCallback(null);
        assertEquals(1, count.get());
    }

    @Test
    void asyncStaticMethod() {
        int prio = GLib.PRIORITY_DEFAULT;
        var cancel = new Cancellable();
        assertTrue(testFunctionSync(prio));

        var hasBeenCalled = new AtomicBoolean(false);
        testFunctionAsync(prio, cancel, (obj, res, _) -> {
            assertNull(obj);
            try {
                hasBeenCalled.set(true);
                assertTrue(testFunctionFinish(res));
            } catch (GErrorException e) {
                fail(e);
            }
        });
        assertEquals(1, testFunctionThawAsync());
        runMainLoopOnce();
        assertTrue(hasBeenCalled.get());
    }

    @Test
    void constructor() {
        var count = new AtomicInteger(41);
        TestCallbackUserData callback = count::incrementAndGet;
        TestObj.callback(callback);
        assertEquals(42, count.get());
        assertEquals(43, testCallbackThawNotifications());
    }

    @Test
    void asyncConstructor() {
        var hasBeenCalled = new AtomicBoolean(false);
        var cancel = new Cancellable();
        TestObj.newAsync("plop", cancel, (obj, res, _) -> {
            hasBeenCalled.set(true);
            assertNull(obj);
            try {
                TestObj result = TestObj.finish(res);
                assertEquals(TestObj.getType(), result.readGClass().readGType());
            } catch (GErrorException e) {
                fail(e);
            }
        });
        assertEquals(1, TestObj.constructorThawAsync());
        runMainLoopOnce();
        assertTrue(hasBeenCalled.get());
    }

    @Test
    void hashTable() {
        var hasBeenCalled = new AtomicBoolean(false);
        HashTable<String, Integer> hashtable = new HashTable<>(
                GLib::strHash,
                GLib::strEqual,
                Interop::getStringFrom,
                segment -> (int) segment.address()
        );
        hashtable.put("a", 1);
        hashtable.put("b", 2);
        hashtable.put("c", 3);
        testHashTableCallback(hashtable, t -> {
            hasBeenCalled.set(true);
            assertEquals(hashtable, t);
        });
        assertTrue(hasBeenCalled.get());
    }

    @Test
    void gerror() {
        var hasBeenCalled = new AtomicBoolean(false);
        testGerrorCallback(e -> {
            hasBeenCalled.set(true);
            assertEquals(e.readDomain(), Gio.ioErrorQuark());
            assertEquals(e.readCode(), IOErrorEnum.NOT_SUPPORTED.getValue());
        });
        assertTrue(hasBeenCalled.get());
    }

    @Test
    void gerrorNull() {
        var hasBeenCalled = new AtomicBoolean(false);
        testNullGerrorCallback(e -> {
            hasBeenCalled.set(true);
            assertNull(e);
        });
        assertTrue(hasBeenCalled.get());
    }

    @Test
    void gerrorOwned() {
        var hasBeenCalled = new AtomicBoolean(false);
        testOwnedGerrorCallback(e -> {
            hasBeenCalled.set(true);
            assertEquals(e.readDomain(), Gio.ioErrorQuark());
            assertEquals(e.readCode(), IOErrorEnum.PERMISSION_DENIED.getValue());
        });
        assertTrue(hasBeenCalled.get());
    }
}
