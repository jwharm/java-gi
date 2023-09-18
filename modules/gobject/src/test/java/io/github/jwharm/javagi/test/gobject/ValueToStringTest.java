package io.github.jwharm.javagi.test.gobject;

import io.github.jwharm.javagi.gobject.types.Types;
import org.gnome.gobject.Value;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code org.gnome.Value.toString()} is injected with a patch.
 * Test if it works.
 * <p>
 * If this test fails because the actual value is something like
 * {@code org.gnome.gobject.Value@31e4bb20}, it means the
 * {@code toString()} method doesn't exist (the patch was not applied).
 */
public class ValueToStringTest {

    @Test
    public void testValueToString() {
        Value vInt = Value.allocate().init(Types.INT);
        vInt.setInt(123);
        assertEquals("123", vInt.toString());

        Value vBool = Value.allocate().init(Types.BOOLEAN);
        vBool.setBoolean(true);
        assertEquals("TRUE", vBool.toString());

        Value vStr = Value.allocate().init(Types.STRING);
        vStr.setString("abc");
        assertEquals("\"abc\"", vStr.toString());
    }
}
