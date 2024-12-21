package io.github.jwharm.javagi.test.gobject;

import io.github.jwharm.javagi.gobject.annotations.Flags;
import io.github.jwharm.javagi.gobject.types.Types;
import org.gnome.gobject.Type;
import org.gnome.gobject.GObjects;
import org.junit.jupiter.api.Test;

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
}
