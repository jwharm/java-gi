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

package io.github.jwharm.javagi.test.gobject;

import io.github.jwharm.javagi.gobject.annotations.Property;
import io.github.jwharm.javagi.gobject.annotations.RegisteredType;
import org.gnome.gobject.GObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test property definitions on a custom GObject-derived class.
 */
public class PropertyTest {

    @Test
    void testCustomProperties() {
        var dino = new Dino();
        var gclass = (GObject.ObjectClass) dino.readGClass();

        // Check that the properties exist
        assertNotNull(gclass.findProperty("foo"));
        assertNotNull(gclass.findProperty("bar"));
        assertNotNull(gclass.findProperty("abc"));
        assertNotNull(gclass.findProperty("fgh"));
        assertNotNull(gclass.findProperty("xyz"));

        // Renamed
        assertNull(gclass.findProperty("baz"));
        assertNotNull(gclass.findProperty("baz2"));

        // Skipped
        assertNull(gclass.findProperty("qux"));

        // Default value (from Java field)
        assertEquals(10, dino.getProperty("abc")); // default
        dino.setProperty("abc", 15);               // update value
        assertEquals(15, dino.getProperty("abc")); // updated value

        // Valid value
        dino.setProperty("abc", 6);
        assertEquals(6, dino.getProperty("abc"));

        /*
         * Tests for invalid values (too low or too high) are disabled here,
         * to prevent CRITICAL GObject errors on the command-line output when
         * executing testcases.
         * Uncomment the following lines to run these tests.
         *
         * // Too low value
         * dino.setProperty("abc", 3);
         * assertEquals(6, dino.getProperty("abc"));
         *
         * // Too high value
         * dino.setProperty("abc", 32);
         * assertEquals(6, dino.getProperty("abc"));
         */

        // Check default value from `@Property` annotation
        assertEquals(30, dino.getProperty("fgh")); // default
        dino.setProperty("fgh", 31);               // update value (ignored)
        assertEquals(30, dino.getProperty("fgh")); // default

        dino.setProperty("xyz", 70L);
        assertEquals(0, dino.getProperty("xyz"));
    }

    @SuppressWarnings("unused")
    @RegisteredType(name="Dino")
    public static class Dino extends GObject{
        private int foo;
        private boolean bar;
        private String baz;
        private float qux;
        private int abc = 10;
        private int fgh;
        private long xyz;

        public Dino() {
            super();
        }

        // Int property with getter and setter
        public int getFoo() {
            return foo;
        }

        public void setFoo(int foo) {
            this.foo = foo;
        }

        // Boolean property with getter (starting with "is") and setter
        public boolean isBar() {
            return bar;
        }

        public void setBar(boolean bar) {
            this.bar = bar;
        }

        // String property with custom name, and getter and setter
        @Property(name="baz2")
        public String readBaz() {
            return baz;
        }

        @Property(name="baz2")
        public void writeBaz(String baz) {
            this.baz = baz;
        }

        // Skipped property
        @Property(skip=true)
        public float getQux() {
            return qux;
        }

        @Property(skip=true)
        public void setQux(float qux) {
            this.qux = qux;
        }

        // Int property with getter and setter, and custom min/max values
        // The default value must be set here as well, because GObject otherwise
        // complains it's 0, which is below the minimum value.
        @Property(minimumValue = "5", defaultValue = "10", maximumValue = "20")
        public int getAbc() {
            return abc;
        }

        @Property
        public void setAbc(int abc) {
            this.abc = abc;
        }

        // Int property with a default value instead of a getter
        @Property(defaultValue = "30")
        public void setFgh(int fgh) {
            this.fgh = fgh;
        }

        // Long property with annotated setter, not-annotated getter
        @Property
        public void setXyz(long Xyz) {
            this.xyz = Xyz;
        }

        // This getter must be ignored because it's not annotated
        public long getXyz() {
            return xyz;
        }
    }
}
