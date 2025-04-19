/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.test.gobject;

import io.github.jwharm.javagi.gobject.annotations.Property;
import io.github.jwharm.javagi.gobject.annotations.RegisteredType;
import io.github.jwharm.javagi.gobject.annotations.Signal;
import org.gnome.gobject.GObject;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test user-defined signals:
 * <ul>
 * <li>Defining a custom signal on a GObject-derived class
 * <li>Connecting to the custom signal
 * <li>Emitting the custom signal
 * </ul>
 */
public class NewSignalTest {

    @Test
    void registerSignal() {
        // Create a Counter object (see below) with limit 10
        Counter counter = new Counter(10);
        AtomicBoolean success = new AtomicBoolean(false);

        // Connect to the "limit-reached" signal
        counter.connect("limit-reached", (Counter.LimitReached) _ -> success.set(true));

        // First count to 9, this should not run the callback.
        for (int a = 0; a < 9; a++)
            counter.count();
        assertFalse(success.get());

        // Now increase the count to 10, the limit. This should run the callback.
        counter.count();
        assertTrue(success.get());
    }

    /**
     * Simple GObject-derived class that can count up to a predefined maximum number.
     * When the maximum number is reached, the "limit-reached" signal is emitted.
     * The class exposes two properties: the current count ("count") and the limit ("limit").
     */
    @RegisteredType(name="TestCounter")
    public static class Counter extends GObject {
        public Counter(int limit) {
            super("limit", 10);
        }

        @Signal
        public interface LimitReached extends IntConsumer {}

        private int num = 0;
        private int limit;

        @Property(name="count", writable=false)
        public int getCount() {
            return num;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getLimit() {
            return this.limit;
        }

        public void count() {
            num++;
            if (num == limit) {
                emit("limit-reached", limit);
            }
        }
    }
}
