package org.javagi.glib;

import org.gnome.glib.GLib;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test calling a variadic function with string, int, float and char values
 */
public class VarargsTest {

    @Test
    void testVarargs() {
        var str = GLib.strdupPrintf("%s %d %.2f %c", "abc", 123, 4.56f, 'c');

        // both are OK, depending on the locale
        var possibleResults = List.of("abc 123 4.56 c", "abc 123 4,56 c");
        assertTrue(possibleResults.contains(str));
    }
}
