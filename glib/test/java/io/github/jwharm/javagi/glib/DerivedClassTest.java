package io.github.jwharm.javagi.glib;

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
        Value input = Value.allocate().init(Types.STRING);
        Value output = Value.allocate().init(Types.STRING);
        input.setString("test value");

        TestObject object = GObject.newInstance(TestObject.gtype);
        object.setProperty("test-property", input);
        object.getProperty("test-property", output);

        assertEquals(input.getString(), output.getString());
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

        private String testProperty = null;

        @Property(name="test-property")
        public String getTestProperty() {
            return testProperty;
        }

        @Property(name="test-property")
        public void setTestProperty(String value) {
            this.testProperty = value;
        }
    }
}
