package io.github.jwharm.javagi.test.glib;

import org.gnome.glib.GLib;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test calling a variadic function
 */
public class VarargsTest {

    @Test
    void testVarargs() {
        var str = GLib.strdupPrintf("%s %d %.2f %c", "abc", 123, 4.56f, 'c');
        assertEquals("abc 123 4,56 c", str);
    }
}
