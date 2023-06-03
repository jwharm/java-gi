package io.github.jwharm.javagi.glib;

import org.gnome.gio.SimpleAction;
import org.gnome.gobject.WeakRef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Create a GWeakRef to a GObject and read it back.
 */
public class WeakRefTest {

    @Test
    public void createWeakRef() {
        SimpleAction gobject = new SimpleAction("test", null);

        @SuppressWarnings("unchecked")
        WeakRef<SimpleAction> weakRef = WeakRef.allocate();
        weakRef.init(gobject);

        SimpleAction action2 = weakRef.get();
        weakRef.clear();

        assertEquals(gobject, action2);
    }
}
