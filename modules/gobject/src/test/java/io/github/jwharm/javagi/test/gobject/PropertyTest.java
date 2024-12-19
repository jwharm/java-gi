package io.github.jwharm.javagi.test.gobject;

import io.github.jwharm.javagi.gobject.annotations.Property;
import io.github.jwharm.javagi.gobject.annotations.RegisteredType;
import io.github.jwharm.javagi.gobject.types.Types;
import org.gnome.gobject.GObject;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;

import static org.junit.jupiter.api.Assertions.*;

public class PropertyTest {

    @Test
    void testCustomProperties() {
        Types.register(Dino.class);
        var dino = GObject.newInstance(Dino.class);
        var gclass = (GObject.ObjectClass) dino.readGClass();

        assertNotNull(gclass.findProperty("foo"));
        assertNotNull(gclass.findProperty("bar"));
        assertNotNull(gclass.findProperty("baz2"));

        assertNull(gclass.findProperty("baz"));
        assertNull(gclass.findProperty("qux"));

        // Valid value
        dino.setProperty("abc", 6);
        assertEquals(6, dino.getProperty("abc"));

        // Too low value
        dino.setProperty("abc", 3);
        assertEquals(6, dino.getProperty("abc"));

        // Too high value
        dino.setProperty("abc", 32);
        assertEquals(6, dino.getProperty("abc"));

        // Update to another valid value
        dino.setProperty("abc", 15);
        assertEquals(15, dino.getProperty("abc"));

        // Check default value
        var spec = gclass.findProperty("abc");
        assertNotNull(spec);
        var defaultValue = spec.getDefaultValue().getInt();
        assertEquals(10, defaultValue);
    }

    @SuppressWarnings("unused")
    @RegisteredType(name="Dino")
    public static class Dino extends GObject{
        private int foo;
        private boolean bar;
        private String baz;
        private float qux;
        private int abc;

        public Dino(MemorySegment address) {
            super(address);
        }

        public int getFoo() {
            return foo;
        }

        public void setFoo(int foo) {
            this.foo = foo;
        }

        public boolean isBar() {
            return bar;
        }

        public void setBar(boolean bar) {
            this.bar = bar;
        }

        @Property(name="baz2")
        public String readBaz() {
            return baz;
        }

        @Property(name="baz2")
        public void writeBaz(String baz) {
            this.baz = baz;
        }

        @Property(skip=true)
        public float getQux() {
            return qux;
        }

        @Property(skip=true)
        public void setQux(float qux) {
            this.qux = qux;
        }

        @Property(minimumValue = "5", defaultValue = "10", maximumValue = "20")
        public int getAbc() {
            return abc;
        }

        @Property(minimumValue = "5", defaultValue = "10", maximumValue = "20")
        public void setAbc(int abc) {
            this.abc = abc;
        }
    }
}
