package io.github.jwharm.javagi.test.gobject;

import io.github.jwharm.javagi.gobject.types.Types;
import org.gnome.glib.Variant;
import org.gnome.gobject.GObject;
import org.gnome.gobject.Value;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * GValue, GType and GVariant have an extra {@code toString()} method.
 * Test if these methods work as expected.
 */
public class ToStringTest {

    @Test
    public void testValueToString() {
        Value vInt = new Value(Arena.ofAuto()).init(Types.INT);
        vInt.setInt(123);
        assertEquals("123", vInt.toString());
        vInt.unset();

        Value vBool = new Value(Arena.ofAuto()).init(Types.BOOLEAN);
        vBool.setBoolean(true);
        assertEquals("TRUE", vBool.toString());
        vBool.unset();

        Value vStr = new Value(Arena.ofAuto()).init(Types.STRING);
        vStr.setString("abc");
        assertEquals("\"abc\"", vStr.toString());
        vStr.unset();
    }

    @Test
    public void testVariantToString() {
        var vInt = new Variant("u", 40);
        assertEquals("uint32 40", vInt.toString());
    }

    @Test
    public void testTypeToString() {
        assertEquals("GThread", Types.THREAD.toString());
        assertEquals("GObject", GObject.getType().toString());
    }
}
