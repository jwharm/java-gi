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

package io.github.jwharm.javagi.test.glib;

import io.github.jwharm.javagi.annotations.Property;
import io.github.jwharm.javagi.types.Types;
import io.github.jwharm.javagi.util.JavaClosure;
import org.gnome.glib.Type;
import org.gnome.gobject.Binding;
import org.gnome.gobject.BindingFlags;
import org.gnome.gobject.GObject;
import org.gnome.gobject.Value;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test a GObject property binding with a transformation closure.
 */
public class ClosureTest {

    @SuppressWarnings("unused")
    public interface MyInterface {
        boolean timesTwo(MemorySegment p1, MemorySegment p2);
    }

    @Test
    public void methodReference() {
        // Create 2 objects, both with a simple "num" property of type int
        NumObject n1 = GObject.newInstance(NumObject.type);
        NumObject n2 = GObject.newInstance(NumObject.type);

        // Create a JavaClosure for the "timesTwo" method
        Method timesTwo = null;
        try {
            timesTwo = ClosureTest.class.getMethod("timesTwo", MemorySegment.class, MemorySegment.class);
        } catch (NoSuchMethodException ignored) {}
        JavaClosure closure = new JavaClosure(this, timesTwo);

        // Create a property binding to run "timesTwo" every time the "num" property on n1 or n2 is changed
        // Keep a reference to the Binding object instance alive, or else the property binding will be disconnected
        @SuppressWarnings("unused")
        Binding binding = n1.bindPropertyFull("num", n2, "num", BindingFlags.BIDIRECTIONAL, closure, closure);

        // Set the "num" property of n1 to 10
        n1.setProperty("num", 10);

        // The "num" property of n2 should now be n1 times two
        assertEquals(20, n2.getNum());
    }

    @Test
    public void lambda() {
        // Create 2 objects, both with a simple "num" property of type int
        NumObject n1 = GObject.newInstance(NumObject.type);
        NumObject n2 = GObject.newInstance(NumObject.type);
        
        // Create a JavaClosure for the "timesTwo" method
        JavaClosure closure = new JavaClosure((MyInterface) this::timesTwo);

        // Create a property binding to run "timesTwo" every time the "num" property on n1 or n2 is changed
        // Keep a reference to the Binding object instance alive, or else the property binding will be disconnected
        @SuppressWarnings("unused")
        Binding binding = n1.bindPropertyFull("num", n2, "num", BindingFlags.BIDIRECTIONAL, closure, closure);

        // Set the "num" property of n1 to 10
        n1.setProperty("num", -25);

        // The "num" property of n2 should now be n1 times two
        assertEquals(-50, n2.getNum());
    }

    // The method that is wrapped in a JavaClosure
    public boolean timesTwo(MemorySegment boxed1, MemorySegment boxed2) {
        Value src = new Value(boxed1);
        Value dest = new Value(boxed2);
        dest.setInt(src.getInt() * 2);
        return true;
    }

    // A simple GObject-derived class with a "num" property
    public static class NumObject extends GObject {
        public static Type type = Types.register(NumObject.class);
        public NumObject(MemorySegment address) {
            super(address);
        }

        private int num;

        @SuppressWarnings("unused")
        @Property(name="num") public void setNum(int num) {
            this.num = num;
        }
        @Property(name="num") public int getNum() {
            return this.num;
        }
    }
}
