/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 Jan-Willem Harmannij
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

import org.javagi.gobject.annotations.ClassInit;
import org.javagi.gobject.annotations.InstanceInit;
import org.javagi.gobject.annotations.Property;
import org.javagi.gobject.annotations.RegisteredType;
import org.javagi.gobject.types.TypeCache;
import org.javagi.gobject.types.Types;
import org.gnome.glib.Type;
import org.gnome.gobject.GObject;
import org.gnome.gobject.GObjects;
import org.gnome.gobject.Value;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test registering new GTypes for a Java class
 */
public class DerivedClassTest {

    /**
     * Check if the name of the  GType is correctly set
     */
    @Test
    public void gtypeNameIsCorrect() {
        assertEquals("JavaGiTestObject", GObjects.typeName(TestObject.gtype));
    }

    private static boolean classInitHasRun = false;
    private static boolean instanceInitHasRun = false;

    /**
     * Check if the initializer functions run
     */
    @Test
    public void initializersHaveRun() {
        TestObject ignored = GObject.newInstance(TestObject.gtype);
        assertTrue(classInitHasRun);
        assertTrue(instanceInitHasRun);
    }

    /**
     * Compare the GType of the instance with the declared class GType
     */
    @Test
    public void instanceGtypeIsCorrect() {
        TestObject object = GObject.newInstance(TestObject.gtype);
        assertEquals(object.readGClass().readGType(), TestObject.gtype);
    }

    /**
     * Write a value to a GObject property and read it back
     */
    @Test
    public void writeAndReadProperty() {
        // With manually created GValues
        Value input = new Value(Arena.ofAuto()).init(Types.STRING);
        Value output = new Value(Arena.ofAuto()).init(Types.STRING);
        input.setString("test value");

        TestObject object = GObject.newInstance(TestObject.gtype);
        object.setProperty("string-property", input);
        object.getProperty("string-property", output);
        assertEquals(input.getString(), output.getString());

        // With convenience methods
        String input2 = "another test value";
        object.setProperty("string-property", input2);
        assertEquals(input2, object.getProperty("string-property"));
    }

    /**
     * Write a boolean to a GObject property and read it back
     */
    @Test
    public void writeAndReadBooleanProperty() {
        // With manually created GValues
        Value input = new Value(Arena.ofAuto()).init(Types.BOOLEAN);
        Value output = new Value(Arena.ofAuto()).init(Types.BOOLEAN);
        input.setBoolean(true);
        TestObject object = GObject.newInstance(TestObject.gtype);
        object.setProperty("bool-property", input);
        object.getProperty("bool-property", output);
        assertEquals(input.getBoolean(), output.getBoolean());

        // With convenience methods
        boolean input2 = false;
        object.setProperty("bool-property", input2);
        assertEquals(input2, object.getProperty("bool-property"));
    }

    @Test
    public void constructWithClassParameter() {
        TestObject object = new TestObject();
        assertEquals(object.readGClass().readGType(), TestObject.gtype);

        String input1 = "abc";
        boolean input2 = true;
        TestObject object2 = GObject.newInstance(TestObject.class,
                "string-property", input1,
                "bool-property", input2);
        assertEquals(input1, object2.getProperty("string-property"));
        assertEquals(input2, object2.getProperty("bool-property"));
    }

    @Test
    public void autoRegisterType() {
        new AutoRegistered();
        assertTrue(TypeCache.contains(AutoRegistered.class));
    }

    @Test
    public void deeplyDerivedClass() {
        new GrandChild();
        Type type = TypeCache.getType(GrandChild.class);
        assertEquals("org_javagi_gobject_DerivedClassTest_GrandChild", type.toString());
    }

    /**
     * Simple GObject-derived class used in the above tests
     */
    @RegisteredType(name="JavaGiTestObject")
    public static class TestObject extends GObject {
        public static Type gtype = Types.register(TestObject.class);
        public TestObject(MemorySegment address) {
            super(address);
        }

        public TestObject() {
            super();
        }

        @ClassInit
        public static void construct(GObject.ObjectClass typeClass) {
            classInitHasRun = true;
        }

        @InstanceInit
        public void init() {
            instanceInitHasRun = true;
        }

        private String stringProperty;

        @Property(name="string-property")
        public String getStringProperty() {
            return stringProperty;
        }

        @Property(name="string-property")
        public void setStringProperty(String value) {
            this.stringProperty = value;
        }

        private boolean boolProperty;

        @Property // name will be inferred: "bool-property"
        public boolean getBoolProperty() {
            return boolProperty;
        }

        @Property
        public void setBoolProperty(boolean boolProperty) {
            this.boolProperty = boolProperty;
        }
    }

    public static class AutoRegistered extends GObject {
    }

    public static class GrandChild extends TestObject {
    }
}
