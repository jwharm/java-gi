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

import org.javagi.gir.Class;
import org.javagi.util.Patch;
import org.javagi.gir.Constructor;
import org.javagi.gir.GirElement;
import org.javagi.gir.Type;

import static java.util.function.Predicate.not;

public class GtkPatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        /*
         * Named constructors of Gtk Widgets often specify return type "Widget".
         * To prevent redundant casts, we override them with the actual type.
         */
        if (element instanceof Class cls) {
            for (Constructor ctor : cls.constructors().stream()
                    .filter(not(f -> f.name().equals("new")))
                    .toList()) {
                var type = (Type) ctor.returnValue().anyType();
                if ("GtkWidget*".equals(type.cType())) {
                    if ("Gtk.Widget".equals(type.name())
                            || ("Gtk".equals(namespace) && "Widget".equals(type.name()))) {
                        type.setAttr("name", cls.name());
                        type.setAttr("c:type", "Gtk" + cls.name() + "*");
                    }
                }
            }
        }

        return element;
    }
}
