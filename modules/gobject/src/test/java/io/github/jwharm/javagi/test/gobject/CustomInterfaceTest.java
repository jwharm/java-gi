/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.test.gobject;

import io.github.jwharm.javagi.base.Proxy;
import io.github.jwharm.javagi.gobject.annotations.Property;
import io.github.jwharm.javagi.gobject.annotations.RegisteredType;
import io.github.jwharm.javagi.gobject.annotations.Signal;
import io.github.jwharm.javagi.gobject.types.Types;
import org.gnome.gobject.Type;
import org.gnome.gobject.GObject;
import org.gnome.gobject.GObjects;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test registering new GTypes for a Java class
 */
public class CustomInterfaceTest {

    /**
     * Check if the name of the  GType is correctly set
     */
    @Test
    public void testCustomInterface() {
        assertTrue(Types.IS_INTERFACE(TestInterface.gtype));
        assertTrue(GObjects.typeIsA(TestObject.gtype, TestInterface.gtype));

        var instance = GObject.newInstance(TestObject.class);
        assertTrue(GObjects.typeCheckInstanceIsA(instance, TestInterface.gtype));
    }

    @Test
    public void testComplexInterface() {
        var instance = GObject.newInstance(BarClass.class);
        assertTrue(GObjects.typeCheckInstanceIsA(instance, FooInterface.gtype));

        var result = new AtomicInteger(0);
        instance.connect("number-changed", (FooInterface.NumberChanged) result::set);
        instance.setProperty("number", 2);
        assertEquals(2, result.get());
    }

    @RegisteredType(name="JavaGiTestInterface")
    public interface TestInterface extends Proxy {
        Type gtype = Types.register(TestInterface.class);
    }

    @RegisteredType(name="JavaGiTestObjectWithInterface")
    public static class TestObject extends GObject implements TestInterface {
        public static Type gtype = Types.register(TestObject.class);
        public TestObject(MemorySegment address) {
            super(address);
        }
    }

    @RegisteredType(name="JavaGiFooInterface",
                    prerequisites = {GObject.class})
    public interface FooInterface extends Proxy {
        Type gtype = Types.register(FooInterface.class);

        @Property
        int getNumber();

        @Property
        void setNumber(int number);

        @Signal
        interface NumberChanged extends IntConsumer {}
    }

    @RegisteredType(name="JavaGiBarClass")
    public static class BarClass extends GObject
            implements FooInterface {
        public static Type gtype = Types.register(BarClass.class);
        public BarClass(MemorySegment address) {
            super(address);
        }

        private int number = 0;

        @Override @Property
        public int getNumber() {
            return number;
        }

        @Override @Property
        public void setNumber(int number) {
            this.number = number;
            emit("number-changed", number);
        }
    }
}
