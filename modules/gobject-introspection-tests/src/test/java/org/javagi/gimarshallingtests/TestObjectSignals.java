/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2025 Jan-Willem Harmannij
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package org.javagi.gimarshallingtests;

import org.gnome.gi.gimarshallingtests.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TestObjectSignals {
    SignalsObject obj;

    @BeforeEach
    void constructObject() {
        obj = new SignalsObject();
    }

    @Test
    void boxedGPtrArrayUtf8() {
        String[] strings = {"0", "1", "2"};
        AtomicInteger count = new AtomicInteger(0);
        obj.onSomeBoxedGptrarrayUtf8(array -> {
            count.incrementAndGet();
            assertArrayEquals(strings, array);
        });
        obj.emitBoxedGptrarrayUtf8();
        assertEquals(1, count.get());
    }

    @Test
    void boxedGPtrArrayUtf8Container() {
        String[] strings = {"0", "1", "2"};
        AtomicInteger count = new AtomicInteger(0);
        obj.onSomeBoxedGptrarrayUtf8Container(array -> {
            count.incrementAndGet();
            assertArrayEquals(strings, array);
        });
        obj.emitBoxedGptrarrayUtf8Container();
        assertEquals(1, count.get());
    }

    @Test
    void boxedGPtrArrayUtf8Full() {
        String[] strings = {"0", "1", "2"};
        AtomicInteger count = new AtomicInteger(0);
        obj.onSomeBoxedGptrarrayUtf8Full(array -> {
            count.incrementAndGet();
            assertArrayEquals(strings, array);
        });
        obj.emitBoxedGptrarrayUtf8Full();
        assertEquals(1, count.get());
    }

    @Test
    void boxedGPtrArrayBoxedStruct() {
        AtomicInteger count = new AtomicInteger(0);
        obj.onSomeBoxedGptrarrayBoxedStruct(array -> {
            count.incrementAndGet();
            assertEquals(3, array.length);
            assertEquals(42, array[0].readLong());
            assertEquals(43, array[1].readLong());
            assertEquals(44, array[2].readLong());
        });
        obj.emitBoxedGptrarrayBoxedStruct();
        assertEquals(1, count.get());
    }

    @Test
    void boxedGPtrArrayBoxedStructContainer() {
        AtomicInteger count = new AtomicInteger(0);
        obj.onSomeBoxedGptrarrayBoxedStructContainer(array -> {
            count.incrementAndGet();
            assertEquals(3, array.length);
            assertEquals(42, array[0].readLong());
            assertEquals(43, array[1].readLong());
            assertEquals(44, array[2].readLong());
        });
        obj.emitBoxedGptrarrayBoxedStructContainer();
        assertEquals(1, count.get());
    }

    @Test
    void boxedGPtrArrayBoxedStructFull() {
        AtomicInteger count = new AtomicInteger(0);
        obj.onSomeBoxedGptrarrayBoxedStructFull(array -> {
            count.incrementAndGet();
            assertEquals(3, array.length);
            assertEquals(42, array[0].readLong());
            assertEquals(43, array[1].readLong());
            assertEquals(44, array[2].readLong());
        });
        obj.emitBoxedGptrarrayBoxedStructFull();
        assertEquals(1, count.get());
    }

    @Test
    void hashTableUtf8Int() {
        AtomicInteger count = new AtomicInteger(0);
        obj.onSomeHashTableUtf8Int(hashtable -> {
            count.incrementAndGet();
            assertEquals(1, hashtable.get("-1"));
            assertEquals(0, hashtable.get("0"));
            assertEquals(-1, hashtable.get("1"));
            assertEquals(-2, hashtable.get("2"));
        });
        obj.emitHashTableUtf8Int();
        assertEquals(1, count.get());
    }

    @Test
    void hashTableUtf8IntContainer() {
        AtomicInteger count = new AtomicInteger(0);
        obj.onSomeHashTableUtf8IntContainer(hashtable -> {
            count.incrementAndGet();
            assertEquals(1, hashtable.get("-1"));
            assertEquals(0, hashtable.get("0"));
            assertEquals(-1, hashtable.get("1"));
            assertEquals(-2, hashtable.get("2"));
        });
        obj.emitHashTableUtf8IntContainer();
        assertEquals(1, count.get());
    }

    @Test
    void hashTableUtf8IntFull() {
        AtomicInteger count = new AtomicInteger(0);
        obj.onSomeHashTableUtf8IntFull(hashtable -> {
            count.incrementAndGet();
            assertEquals(1, hashtable.get("-1"));
            assertEquals(0, hashtable.get("0"));
            assertEquals(-1, hashtable.get("1"));
            assertEquals(-2, hashtable.get("2"));
        });
        obj.emitHashTableUtf8IntFull();
        assertEquals(1, count.get());
    }

    @Test
    void boxedStruct() {
        AtomicInteger count = new AtomicInteger(0);
        obj.onSomeBoxedStruct(struct -> {
            count.incrementAndGet();
            assertEquals(99, struct.readLong());
            assertEquals("a string", struct.readString());
            assertArrayEquals(new String[] {"foo", "bar", "baz"}, struct.readGStrv());
        });
        obj.emitBoxedStruct();
        assertEquals(1, count.get());
    }
}
