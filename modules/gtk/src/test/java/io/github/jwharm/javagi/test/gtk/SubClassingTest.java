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

package io.github.jwharm.javagi.test.gtk;

import io.github.jwharm.javagi.gobject.types.TypeCache;
import org.gnome.gtk.FlowBox;
import org.gnome.gtk.Gtk;
import org.gnome.gtk.SignalListItemFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubClassingTest {

    /**
     * Create a subclass for a class that is final/opaque in native code.
     * The resulting class instances will have the GType of the parent
     * class. The subclass is only defined in Java.
     */
    @Test
    void testFinalSubclass() {
        Gtk.init();

        // FlowBox has no TypeClass.
        // Subclassing should not throw an exception.
        new FlowBoxExtended();

        // The new subclass should have the parent GType
        assertEquals(
                TypeCache.getType(FlowBox.class),
                TypeCache.getType(FlowBoxExtended.class)
        );

        // SignalListItemFactory has a TypeClass, but without a memory layout.
        // Subclassing should not throw an exception.
        new ItemFactoryExtended();
    }

    public static class FlowBoxExtended extends FlowBox {
        public FlowBoxExtended() {
            super();
        }
    }

    public static class ItemFactoryExtended extends SignalListItemFactory {
        public ItemFactoryExtended() {
            super();
        }
    }
}
