package io.github.jwharm.javagi.glib;

import io.github.jwharm.javagi.types.Types;
import org.gnome.gobject.Value;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ValueTest {

    @Test
    public void verifyGValue() {
        Value value = Value.allocate().init(Types.INT);
        value.setInt(15);
        assertEquals(value.getInt(), 15);
    }
}
