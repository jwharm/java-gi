package io.github.jwharm.javagi.test.gtk;

import org.gnome.gtk.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MutableListTest {
    @Test
    public void testMutableList() {
        Gtk.init();

        AnyFilter list = new AnyFilter();
        assertEquals(0, list.size());

        list.add(StringFilter.builder().setSearch("a").build());
        list.add(StringFilter.builder().setSearch("b").build());
        list.add(StringFilter.builder().setSearch("c").build());

        assertEquals(3, list.size());

        list.set(0, StringFilter.builder().setSearch("d").build());

        assertEquals(3, list.size());
        assertEquals("d", list.get(0).toString());

        list.add(1, StringFilter.builder().setSearch("e").build());

        assertEquals(4, list.size());
        assertEquals("d", list.get(0).toString());
        assertEquals("e", list.get(1).toString());
        assertEquals("b", list.get(2).toString());

        list.remove(1);

        assertEquals(3, list.size());
        assertEquals("d", list.get(0).toString());
        assertEquals("b", list.get(1).toString());

        list.clear();

        assertEquals(0, list.size());
    }
}
