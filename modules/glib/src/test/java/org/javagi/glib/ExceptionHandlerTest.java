package org.javagi.glib;

import org.gnome.glib.GLib;
import org.javagi.base.CallbackInvocationException;
import org.javagi.base.ExceptionHandler;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test handling of exceptions from callback methods
 */
public class ExceptionHandlerTest {
    @Test
    public void testExceptionHandling() {
        var success = new AtomicBoolean();

        assertNull(ExceptionHandler.getUncaughtExceptionHandler(), "Another exception handler was set");

        ExceptionHandler.setUncaughtExceptionHandler((e, source) -> {
            /*
             * The asserts in the handler don't *directly* cause the test to
             * fail, because all exceptions thrown by the handler (including
             * AssertionFailedErrors) will be silently ignored by Java-GI.
             * However, if any of the assertions fails, the `success` variable
             * will not be updated.
             */
            assertInstanceOf(RuntimeException.class, e);
            assertEquals("Exception from callback", e.getMessage());
            assertEquals("CompareDataFunc", source);
            success.set(true);
        });

        assertInstanceOf(ExceptionHandler.UncaughtExceptionHandler.class,
                         ExceptionHandler.getUncaughtExceptionHandler(),
                         "Exception handler not set");

        try {
            var array = new MemorySegment[] {MemorySegment.NULL, MemorySegment.NULL};
            GLib.sortArray(array, array.length, (_, _) -> {
                throw new RuntimeException("Exception from callback");
            });
            assertTrue(success.get(), "Exception handler did not run successfully");
        } catch (CallbackInvocationException e) {
            fail("Exception should have been handled by the handler");
        } finally {
            ExceptionHandler.setUncaughtExceptionHandler(null);
        }
    }

    @Test
    public void testExceptionPropagation() {
        try {
            var array = new MemorySegment[] {MemorySegment.NULL, MemorySegment.NULL};
            GLib.sortArray(array, array.length, (_, _) -> {
                throw new RuntimeException("Exception from callback");
            });
            fail("Exception should have been thrown");
        } catch (CallbackInvocationException e) {
            assertEquals("Exception from callback", e.getCause().getMessage());
        }
    }
}
