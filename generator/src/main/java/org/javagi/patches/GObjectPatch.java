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

package org.javagi.patches;

import org.javagi.gir.Field;
import org.javagi.gir.GirElement;
import org.javagi.gir.Record;
import org.javagi.util.Patch;
import org.javagi.gir.Type;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

public class GObjectPatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        if (!"GObject".equals(namespace))
            return element;

        /*
         * Change GInitiallyUnownedClass struct to refer to GObjectClass. Both
         * structs are identical, so this has no practical consequences,
         * besides convincing the bindings generator that
         * GObject.InitiallyUnownedClass is not a fundamental type class, but
         * extends GObject.ObjectClass.
         */
        if (element instanceof Record r
                && "InitiallyUnownedClass".equals(r.name())) {
            Type type = new Type(
                    Map.of("name", "GObject.ObjectClass",
                           "c:type", "GObjectClass"),
                    emptyList()
            );
            Field field = new Field(
                    Map.of("name", "parent_class"),
                    List.of(type)
            );
            return r.withChildren(r.infoElements().doc(), field);
        }

        return element;
    }
}
