package org.javagi.glib;

import org.gnome.glib.*;
import org.javagi.base.GErrorException;
import org.javagi.interop.Interop;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test GHashTable wrapper class
 */
public class HashTableTest {

    @Test
    void testStringHashTable() {
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

    @Test
    void testEnumHashTable() {
        var table1 = new HashTable<>(
                GLib::strHash,
                GLib::strEqual,
                Interop::getStringFrom,
                ChecksumType::of);
        table1.put("enum", ChecksumType.MD5);
        assertEquals(ChecksumType.MD5, table1.get("enum"));
    }

    @Test
    void testFlagsHashTable() {
        var set = Set.of(IOFlags.IS_READABLE, IOFlags.IS_WRITABLE);
        var table2 = new HashTable<>(
                GLib::strHash,
                GLib::strEqual,
                Interop::getStringFrom,
                IOFlags::setOf);
        table2.put("flags", set);
        assertEquals(set, table2.get("flags"));
    }
}
