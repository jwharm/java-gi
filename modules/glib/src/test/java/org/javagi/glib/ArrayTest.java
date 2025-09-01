package org.javagi.glib;

import org.javagi.base.Out;
import org.javagi.interop.Interop;
import org.gnome.glib.GLib;
import org.gnome.glib.HashTable;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ArrayTest {

    /**
     * Test reading values from a GPtrArray
     */
    @Test
    void testArray() {
        try (var arena = Arena.ofConfined()) {
            var table = HashTable.new_(GLib::strHash, GLib::strEqual);
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

    /**
     * Test writing to and reading from an inout array parameter
     */
    @Test
    void testArrayInOut() {
        String input = "c3RyaW5nIHRvIGRlY29kZQ==";
        String base64Decoded = "string to decode";

        Out<byte[]> bytesOut = new Out<>();
        bytesOut.set(input.getBytes(StandardCharsets.UTF_8));
        GLib.base64DecodeInplace(bytesOut);
        String decodedString = new String(bytesOut.get());
        assertEquals(base64Decoded, decodedString);
    }
}
