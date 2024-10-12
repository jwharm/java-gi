package io.github.jwharm.javagi.test.glib;

import io.github.jwharm.javagi.base.GErrorException;
import org.gnome.glib.HashTable;
import org.gnome.glib.Uri;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test GHashTable wrapper class
 */
public class HashTableTest {

    @Test
    void testHashTable() {
        try {
            HashTable<String, String> hashTable = Uri.parseParams("name=john&age=41", -1, "&");
            assertEquals(2, hashTable.size());
            assertTrue(hashTable.contains("name"));
            assertTrue(hashTable.contains("age"));
            assertEquals("john", hashTable.get("name"));
            assertEquals("41", hashTable.lookup("age"));
        } catch (GErrorException e) {
            fail(e);
        }
    }
}
