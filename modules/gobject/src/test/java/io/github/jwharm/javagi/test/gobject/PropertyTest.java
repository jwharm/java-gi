package io.github.jwharm.javagi.test.gobject;

import io.github.jwharm.javagi.gobject.annotations.Property;
import io.github.jwharm.javagi.gobject.annotations.RegisteredType;
import io.github.jwharm.javagi.gobject.types.Types;
import org.gnome.gobject.GObject;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test property definitions on a custom GObject-derived class.
 */
public class PropertyTest {

    @Test
    void testCustomProperties() {
        Types.register(Dino.class);
        var dino = GObject.newInstance(Dino.class);
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

        // Too low value
        dino.setProperty("abc", 3);
        assertEquals(6, dino.getProperty("abc"));

        // Too high value
        dino.setProperty("abc", 32);
        assertEquals(6, dino.getProperty("abc"));

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

        public Dino(MemorySegment address) {
            super(address);
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

        @Property(minimumValue = "5", defaultValue = "10", maximumValue = "20")
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
