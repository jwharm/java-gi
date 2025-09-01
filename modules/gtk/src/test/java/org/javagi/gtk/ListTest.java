package org.javagi.gtk;

import org.gnome.glib.List;
import org.gnome.gtk.Gtk;
import org.gnome.gtk.Window;
import org.gnome.gtk.WindowGroup;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.ListIterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test GList wrapper class
 */
public class ListTest {

    @Test
    public void testList() {
        Gtk.init();

        // Generate a glist with 10 elements
        ArrayList<Window> input = new ArrayList<>();
        for (int i = 0; i < 10; i++)
            input.add(new Window());

        WindowGroup group = new WindowGroup();
        for (Window win : input)
            group.addWindow(win);

        // Check glist size
        List<Window> glist = group.listWindows();
        assertEquals(10, glist.size());

        // Check all elements are in the glist
        for (Window win : glist)
            assertTrue(input.contains(win));

        // Add and remove element at head
        Window newElem = new Window();
        ListIterator<Window> iter = glist.listIterator();
        iter.add(newElem);

        assertEquals(11, glist.size());
        assertEquals(newElem, glist.getFirst());

        iter.next();
        iter.previous();
        iter.remove();

        assertEquals(10, glist.size());

        // Add element in between
        iter.next();
        iter.next();
        iter.add(newElem);
        iter.next();
        assertEquals(newElem, iter.previous());

        // Empty list
        List<Window> emptyList = new WindowGroup().listWindows();
        assertEquals(0, emptyList.size());
        assertTrue(emptyList.isEmpty());
    }
}
