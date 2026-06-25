package org.javagi.glib;

import org.gnome.glib.GLib;
import org.javagi.base.CallbackInvocationException;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test propagation of exceptions from callback methods
 */
public class ExceptionHandlerTest {

    @Test
    public void testExceptionFromCallback() {
        try {
            var array = new MemorySegment[] {MemorySegment.NULL, MemorySegment.NULL};
            GLib.sortArray(array, array.length, (_, _) -> {
                throw new RuntimeException("Exception from callback");
            });
            fail();
        } catch (CallbackInvocationException e) {
            assertEquals("Exception from callback", e.getCause().getMessage());
        }
    }
}
