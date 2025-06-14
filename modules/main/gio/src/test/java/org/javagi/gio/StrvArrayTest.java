package org.javagi.gio;

import org.javagi.interop.Platform;
import org.gnome.gio.DesktopAppInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Get a String[][] array from a Gio function, and check that it contains
 * usable data.
 */
public class StrvArrayTest {

    @Test
    public void testStrvArrayToJava() {
        // DesktopAppInfo is only available on Linux
        assumeTrue(Platform.LINUX.equals(Platform.getRuntimePlatform()));

        // Unless there are absolutely no applications installed, searching
        // for "e" should return a few usable results
        String[][] array = DesktopAppInfo.search("e");
        assertNotNull(array);
        for (String[] inner : array) {
            assertNotNull(inner);
            for (String str : inner) {
                // Check for NULL
                assertNotNull(str);

                // Check for valid strings
                assertTrue(str.endsWith(".desktop"));
            }
        }
    }
}
