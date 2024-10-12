package io.github.jwharm.javagi.test.glib;

import io.github.jwharm.javagi.interop.Interop;
import org.gnome.glib.GLib;
import org.gnome.glib.HashTable;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test reading values from a GPtrArray
 */
public class ArrayTest {

    @Test
    void testArray() {
        try (var arena = Arena.ofConfined()) {
            HashTable table = HashTable.new_(GLib::strHash, GLib::strEqual);
            for (int i = 0; i < 3; i++)
                table.insert(
                        Interop.allocateNativeString("key" + i, arena),
                        Interop.allocateNativeString("val" + i, arena));
            var values = table.getValuesAsPtrArray();
            assertNotNull(values);
            assertEquals(3, values.length);
            for (int i = 0; i < 3; i++) {
                String str = Interop.getStringFrom(values[i]);
                assertEquals(4, str.length());
                assertTrue(str.startsWith("val"));
            }
        }
    }
}
