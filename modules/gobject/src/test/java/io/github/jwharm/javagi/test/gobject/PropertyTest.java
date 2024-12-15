package io.github.jwharm.javagi.test.gobject;

import io.github.jwharm.javagi.gobject.annotations.Property;
import io.github.jwharm.javagi.gobject.annotations.RegisteredType;
import io.github.jwharm.javagi.gobject.types.Types;
import org.gnome.gobject.GObject;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    }

    @SuppressWarnings("unused")
    @RegisteredType(name="Dino")
    public static class Dino extends GObject{
        private int foo;
        private boolean bar;
        private String baz;
        private float qux;

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
    }
}
