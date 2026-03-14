package org.javagi.glib;

import org.javagi.interop.Interop;
import org.junit.jupiter.api.Test;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test what happens when a function is called that does not exist.
 */
public class MethodHandleTest {

    @Test
    void nonExistentFunction() {
        String name = "foo_bar_baz";
        FunctionDescriptor fdesc = FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS);
        MethodHandle mh = Interop.downcallHandle(name, fdesc, false);
        try {
            mh.invokeExact(1, MemorySegment.NULL);
            fail("Non existent function should throw exception");
        } catch (Throwable err) {
            assertInstanceOf(UnsupportedOperationException.class, err);
            var uoe = (UnsupportedOperationException) err;
            assertEquals("Cannot find function '" + name + "'", uoe.getMessage());
        }
    }
}
