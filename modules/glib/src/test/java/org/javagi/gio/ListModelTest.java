package org.javagi.gio;

import org.gnome.gio.DBusMessage;
import org.gnome.gio.Gio;
import org.gnome.gio.ListStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test some operations on ListIndexModel and ListStore
 */
public class ListModelTest {

    // Make sure the GIO library is loaded
    @BeforeAll
    public static void ensureInitialized() {
        Gio.javagi$ensureInitialized();
    }

    @Test
    public void createListModel() {
        // verify that ListIndexModel works as expected
        var listIndexModel = new ListIndexModel(1000);
        assertEquals(listIndexModel.getItemType(), ListIndexModel.ListIndex.getType());
        assertEquals(1000, listIndexModel.getNItems());

        var item500 = listIndexModel.getItem(500);
        assertNotNull(item500);
        assertEquals(500, item500.getIndex());

        // foreach loop on a ListModel
        int i = 0;
        for (var item : listIndexModel)
            assertEquals(item.getIndex(), i++);

        // stream() operations on a ListModel
        var product = listIndexModel.stream()
                .map(ListIndexModel.ListIndex::getIndex)
                .reduce(0, Integer::sum);
        assertEquals(499500, product);
    }

    @Test
    public void createListStore() {
        var listStore = new ListStore<DBusMessage>(DBusMessage.getType());
        var item = new DBusMessage();
        listStore.append(item);
        assertEquals(1, listStore.size());
        assertEquals(item, listStore.getFirst());
    }
}
