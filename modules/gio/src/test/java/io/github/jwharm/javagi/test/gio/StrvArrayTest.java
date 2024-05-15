package io.github.jwharm.javagi.test.gio;

import org.gnome.gio.DesktopAppInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Get a String[][] array from a Gio function, and check that it contains
 * usable data.
 */
public class StrvArrayTest {

    @Test
    public void testStrvArrayToJava() {
        String[][] array = DesktopAppInfo.search("gnome");
        assertNotNull(array);
        String result = "";
        for (String[] inner : array) {
            assertNotNull(inner);
            for (String str : inner) {
                if (str.contains("org.gnome"))
                    result = str;
            }
        }
        assertNotEquals("", result);
    }
}
