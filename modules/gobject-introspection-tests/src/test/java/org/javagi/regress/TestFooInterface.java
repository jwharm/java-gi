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

import org.gnome.gi.regress.FooInterface;
import org.gnome.gi.regress.FooSubInterface;
import org.gnome.gi.regress.Regress;
import org.gnome.gobject.Callback;
import org.gnome.gobject.GObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class TestFooInterface {
    public static class Impl extends GObject implements FooInterface {
        public boolean doRegressFooCalled = false;

        @Override
        public void doRegressFoo(int foo) {
            assertEquals(777, foo);
            this.doRegressFooCalled = true;
        }
    }

    // Interfaces don't load the native library automatically
    @BeforeAll
    static void ensureInitialized() {
        Regress.javagi$ensureInitialized();
    }

    @Test
    void interfaceStaticMethod() {
        // Java doesn't expose static interface methods in the implementing
        // classes, so we can't do this:
        // Impl.staticMethod(77);
        FooInterface.staticMethod(77);
    }

    @Test
    void interfaceOverrideMethod() {
        var o = new Impl();
        o.doRegressFoo(777);
        assertTrue(o.doRegressFooCalled);
    }

    public static class SubImpl extends GObject implements FooInterface, FooSubInterface {
        public boolean doRegressFooCalled = false;
        public boolean doBarCalled = false;

        @Override
        public void doRegressFoo(int foo) {
            assertEquals(777, foo);
            this.doRegressFooCalled = true;
        }

        @Override
        public void doBar() {
            this.doBarCalled = true;
        }
    }

    @Test
    void subInterfaceOverrideMethods() {
        var o = new SubImpl();
        o.doRegressFoo(777);
        assertTrue(o.doRegressFooCalled);

        o.doBar();
        assertTrue(o.doBarCalled);
    }

    @Test
    void destroyEvent() {
        var hasBeenCalled = new AtomicBoolean();
        FooSubInterface.DestroyEventCallback callback = () -> hasBeenCalled.set(true);

        hasBeenCalled.set(false);
        var o1 = new SubImpl();
        o1.onDestroyEvent(callback);
        o1.emitDestroyEvent();
        assertTrue(hasBeenCalled.get());

        hasBeenCalled.set(false);
        var o2 = new SubImpl();
        o2.connect("destroy-event", callback);
        o2.emit("destroy-event");
        assertTrue(hasBeenCalled.get());
    }

    public static class BazImpl extends GObject implements FooInterface, FooSubInterface {
        public boolean doBazCalled = false;

        @Override
        public void doBaz(Callback callback) {
            callback.run();
            this.doBazCalled = true;
        }
    }

    @Test
    void interfaceWithVfuncThatTakesACallback() {
        var hasBeenCalled = new AtomicBoolean(false);
        var o = new BazImpl();
        o.doBaz(() -> hasBeenCalled.set(true));
        assertTrue(hasBeenCalled.get());
        assertTrue(o.doBazCalled);
    }
}
