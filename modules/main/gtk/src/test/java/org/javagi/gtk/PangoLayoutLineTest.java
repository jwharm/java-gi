package org.javagi.gtk;

import org.gnome.glib.SList;
import org.gnome.gtk.Gtk;
import org.gnome.gtk.Label;
import org.gnome.pango.Context;
import org.gnome.pango.Layout;
import org.gnome.pango.LayoutLine;
import org.gnome.pango.LayoutRun;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test reading an {@link SList} from a field.
 */
public class PangoLayoutLineTest {

    @Test
    public void testPangoLayoutLine() {
        Gtk.init();

        String TEST_STR = "test";
        int TEST_LENGTH = TEST_STR.length();

        Label label = new Label();
        Context context = label.getPangoContext();
        Layout layout = new Layout(context);
        layout.setText(TEST_STR, TEST_LENGTH);
        SList<LayoutLine> lines = layout.getLinesReadonly();
        LayoutLine line = lines.getFirst();
        SList<LayoutRun> runs = line.readRuns();
        LayoutRun run = runs.getFirst();
        assertEquals(TEST_LENGTH, run.readItem().readNumChars());
    }
}
