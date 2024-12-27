package io.github.jwharm.javagi.test.gtk;

import org.gnome.gtk.Gtk;
import org.gnome.gtk.StringList;
import org.gnome.gtk.StringObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpliceableListTest {
    @Test
    public void testSpliceableList() {
        Gtk.init();

        StringList list = new StringList();
        assertEquals(0, list.size());

        list.append("a");
        list.append("b");
        list.append("c");

        assertEquals(3, list.size());

        list.set(0, new StringObject("d"));

        assertEquals(3, list.size());
        assertEquals("d", list.getFirst().toString());

        list.add(1, new StringObject("e"));

        assertEquals(4, list.size());
        assertEquals("d", list.get(0).toString());
        assertEquals("e", list.get(1).toString());
        assertEquals("b", list.get(2).toString());

        list.clear();

        assertEquals(0, list.size());

        list.addAll(List.of(new StringObject("a"), new StringObject("b"), new StringObject("c")));

        assertEquals(3, list.size());
        assertEquals("a", list.get(0).toString());
        assertEquals("b", list.get(1).toString());
        assertEquals("c", list.get(2).toString());

        list.removeAt(1);

        assertEquals(2, list.size());

        list.addAll(1, List.of(new StringObject("d"), new StringObject("e")));

        assertEquals(4, list.size());
        assertEquals("a", list.get(0).toString());
        assertEquals("d", list.get(1).toString());
        assertEquals("e", list.get(2).toString());
        assertEquals("c", list.get(3).toString());
    }
}
