/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2026 Jan-Willem Harmannij
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

package org.javagi.gobject;

import org.gnome.gobject.GObject;
import org.gnome.gobject.GObjects;
import org.javagi.gobject.annotations.InstanceInit;
import org.javagi.gobject.annotations.RegisteredType;
import org.javagi.gobject.types.Types;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeepDerivedClassTest {

    private static final AtomicBoolean aInitialized = new AtomicBoolean(false);
    private static final AtomicBoolean bInitialized = new AtomicBoolean(false);

    @Test
    public void testDerived() {
        GObjects.javagi$ensureInitialized();
        Types.register(A.class);
        Types.register(B.class);
        new B();
        assertTrue(bInitialized.get());
        assertTrue(aInitialized.get());
    }

    @RegisteredType(name="TestA")
    public static class A extends GObject {
        @InstanceInit
        public void initA() {
            aInitialized.set(true);
        }

        public A() {
            super();
        }

        public A(MemorySegment address) {
            super(address);
        }
    }

    @RegisteredType(name="TestB")
    public static class B extends A {
        @InstanceInit
        public void initB() {
            bInitialized.set(true);
        }

        public B() {
            super();
        }

        public B(MemorySegment address) {
            super(address);
        }
    }
}
