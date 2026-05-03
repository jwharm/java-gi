/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2026 Jan-Willem Harmannij
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

package org.javagi.patches;

import org.javagi.util.Patch;
import org.javagi.gir.*;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

public class GstPatch implements Patch {

    // Utility function to quickly create a <type name="gint" c:type="gint"/>
    private static Type gintType() {
        return new Type(Map.of("name", "gint", "c:type", "gint"), emptyList());
    }

    @Override
    public GirElement patch(GirElement element, String namespace) {

        /*
         * GstMapFlags is an "extendable" bitfield type. Flags values are added
         * in other namespaces. Java doesn't allow extending an enum, so we
         * generate integer constants instead.
         */

        // Replace all references to GstMapFlags with integers
        if (element instanceof Type t && "GstMapFlags".equals(t.cType()))
            return gintType();

        if (!"Gst".equals(namespace))
            return element;

        if (element instanceof Namespace ns) {
            // Add integer constants for all GstMapFlags members
            ns = add(ns, new Constant(
                    Map.of("name", "MAP_READ", "value", "1"),
                    List.of(new Doc("map for read access"), gintType())));
            ns = add(ns, new Constant(
                    Map.of("name", "MAP_WRITE", "value", "2"),
                    List.of(new Doc("map for write access"), gintType())));
            ns = add(ns, new Constant(
                    Map.of("name", "MAP_FLAG_LAST", "value", "65536"),
                    List.of(new Doc("first flag that can be used for custom purposes"), gintType())));

            // Remove GstMapFlags
            return remove(ns, Bitfield.class, "name", "MapFlags");
        }

        return element;
    }
}
