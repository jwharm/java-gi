package org.javagi.glib;

import org.javagi.interop.Interop;
import org.gnome.glib.AsciiType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.gnome.glib.AsciiType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test conversion of int to EnumSet and back
 */
public class FlagsTest {

    @Test
    void testFlags() {
        int input = 1 + 2 + 4 + 8 + 64 + 512;
        var set = Interop.intToEnumSet(AsciiType.class, AsciiType::of, input);
        assertEquals(Set.of(ALNUM, ALPHA, CNTRL, DIGIT, PRINT, UPPER), set);
        int output = Interop.enumSetToInt(set);
        assertEquals(input, output);
    }
}
