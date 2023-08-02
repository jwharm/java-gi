package io.github.jwharm.javagi.test.glib;

import io.github.jwharm.javagi.annotations.ClassInit;
import io.github.jwharm.javagi.annotations.InstanceInit;
import io.github.jwharm.javagi.annotations.Property;
import io.github.jwharm.javagi.annotations.RegisteredType;
import io.github.jwharm.javagi.types.Types;
import org.gnome.glib.Type;
import org.gnome.gobject.GObject;
import org.gnome.gobject.GObjects;
import org.gnome.gobject.Value;
import org.junit.jupiter.api.Test;

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
        assertEquals(GObjects.typeName(TestObject.gtype), "JavaGiTestObject");
    }

    private static boolean classInitHasRun = false;
    private static boolean instanceInitHasRun = false;

    /**
     * Check if the initializer functions run
     */
    @Test
    public void initializersHaveRun() {
        TestObject object = GObject.newInstance(TestObject.gtype);
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
        Value input = Value.allocate().init(Types.STRING);
        Value output = Value.allocate().init(Types.STRING);
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
        Value input = Value.allocate().init(Types.BOOLEAN);
        Value output = Value.allocate().init(Types.BOOLEAN);
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

    /**
     * Simple GObject-derived class used in the above tests
     */
    @RegisteredType(name="JavaGiTestObject")
    public static class TestObject extends GObject {
        public static Type gtype = Types.register(TestObject.class);
        public TestObject(MemorySegment address) {
            super(address);
        }

        @ClassInit
        public static void construct(GObject.ObjectClass typeClass) {
            classInitHasRun = true;
        }

        @InstanceInit
        public void init() {
            instanceInitHasRun = true;
        }

        private String stringProperty = null;

        @Property(name="string-property")
        public String getStringProperty() {
            return stringProperty;
        }

        @Property(name="string-property")
        public void setStringProperty(String value) {
            this.stringProperty = value;
        }

        private boolean boolProperty = false;

        @Property(name="bool-property")
        public boolean getBoolProperty() {
            return boolProperty;
        }

        @Property(name="bool-property")
        public void setBoolProperty(boolean boolProperty) {
            this.boolProperty = boolProperty;
        }
    }
}
