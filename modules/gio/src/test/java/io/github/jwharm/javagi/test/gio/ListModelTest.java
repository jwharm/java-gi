package io.github.jwharm.javagi.test.gio;

import io.github.jwharm.javagi.gio.ListIndexModel;
import org.gnome.gio.Gio;
import org.gnome.gio.ListModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test some operations on ListIndexModel
 */
public class ListModelTest {

    @Test
    public void createListModel() {
        // explicitly trigger initialization
        Gio.javagi$ensureInitialized();

        ListModel listIndexModel = ListIndexModel.newInstance(1000);
        assertEquals(listIndexModel.getItemType(), ListIndexModel.ListIndex.getType());
        assertEquals(1000, listIndexModel.getNItems());

        var item500 = (ListIndexModel.ListIndex) listIndexModel.getItem(500);
        assertNotNull(item500);
        assertEquals(500, item500.getIndex());
    }
}
