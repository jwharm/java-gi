/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.util.Patch;
import io.github.jwharm.javagi.util.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;

public class GdkPatch implements Patch {
    @Override
    public GirElement patch(GirElement element, String namespace) {

        if (!"Gdk".equals(namespace))
            return element;

        /*
         * The GdkModifierType documentation states:
         *   Note that GDK may add internal values to events which include
         *   values outside of this enumeration. Your code should preserve and
         *   ignore them.
         *
         * We preserve and ignore the internal flags in our Java EnumSet by
         * adding placeholder "INTERNAL_n" flags for the missing ones.
         */
        if (element instanceof Bitfield bf
                && bf.platforms() == Platform.ALL
                && "ModifierType".equals(bf.name())) {
            var children = new ArrayList<>(bf.children());
            Doc doc = new Doc(emptyMap(),
                    "Internal flag. Your code should preserve and ignore this flag.");
            for (int bit = 0; bit < 31; bit++) {
                var value = Integer.toString(1 << bit);
                if (bf.members().stream().noneMatch(m -> m.value().equals(value)))
                    children.add(bit + 2, new Member(
                            Map.of("name", "INTERNAL_" + bit, "value", value),
                            List.of(doc)));
            }
            return element.withChildren(children);
        }

        return element;
    }
}
