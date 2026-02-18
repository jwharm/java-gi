package org.javagi.gobject;

import org.gnome.gobject.GObject;
import org.javagi.gobject.annotations.Flags;
import org.javagi.gobject.types.Types;
import org.gnome.glib.Type;
import org.gnome.gobject.GObjects;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test custom registered enum and flags types
 */
public class CustomEnumTest {

    @Test
    public void testCustomEnum() {
        var foo = MyEnum.FOO;
        var bar = MyEnum.BAR;
        var baz = MyEnum.BAZ;

        assertNotNull(foo);
        assertNotNull(bar);
        assertNotNull(baz);

        assertEquals(foo.toString(), GObjects.enumToString(MyEnum.getType(), 0));
        assertEquals(bar.toString(), GObjects.enumToString(MyEnum.getType(), 1));
        assertEquals(baz.toString(), GObjects.enumToString(MyEnum.getType(), 2));
    }

    @Test
    public void testCustomFlags() {
        var foo = MyFlags.FOO;
        var bar = MyFlags.BAR;
        var baz = MyFlags.BAZ;

        assertNotNull(foo);
        assertNotNull(bar);
        assertNotNull(baz);

        assertEquals("FOO | BAR | BAZ",
                     GObjects.flagsToString(MyFlags.getType(), 1|2|4));
    }

    public enum MyEnum {
        FOO,
        BAR,
        BAZ;

        private static final Type gtype = Types.register(MyEnum.class);

        public static Type getType() {
            return gtype;
        }
    }

    @Flags
    public enum MyFlags {
        FOO,
        BAR,
        BAZ;

        private static final Type gtype = Types.register(MyFlags.class);

        public static Type getType() {
            return gtype;
        }
    }

    @SuppressWarnings("unused")
    public static class GTestObject extends GObject {
        private MyEnum testEnum;
        private Set<MyFlags> testFlags;
        private Set<Integer> unsupportedType;

        public MyEnum getTestEnum() {
            return testEnum;
        }

        public void setTestEnum(MyEnum testEnum) {
            this.testEnum = testEnum;
        }

        public Set<MyFlags> getTestFlags() {
            return testFlags;
        }

        public void setTestFlags(Set<MyFlags> testFlags) {
            this.testFlags = testFlags;
        }

        public void setUnsupportedType(Set<Integer> set) {
            this.unsupportedType = set;
        }

        public Set<Integer> getUnsupportedType() {
            return unsupportedType;
        }
    }

    @Test
    public void testEnumProperty() {
        var obj = new GTestObject();
        obj.setProperty("test-enum", MyEnum.BAR);
        assertEquals(MyEnum.BAR, obj.getProperty("test-enum"));
    }

    @Test
    public void testFlagProperty() {
        var obj = new GTestObject();
        obj.setProperty("test-flags", EnumSet.of(MyFlags.FOO, MyFlags.BAZ));
        assertEquals(Set.of(MyFlags.FOO, MyFlags.BAZ), obj.getProperty("test-flags"));
    }
}
