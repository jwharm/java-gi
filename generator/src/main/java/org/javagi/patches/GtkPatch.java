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

import org.javagi.configuration.ClassNames;
import org.javagi.gir.*;
import org.javagi.gir.Class;
import org.javagi.util.Patch;

public class GtkPatch implements Patch {
    @Override
    public void patchRepository(Repository repository) {
        Namespace ns = repository.namespace();

        /*
         * Named constructors of Gtk Widgets often specify return type "Widget".
         * To prevent redundant casts, we override them with the actual type.
         */
        for (Node node : ns.select("*#class", "*#constructor")) {
            var ctor = (Constructor) node;
            if ("new".equals(ctor.name()))
                continue;

            var type = (Type) ctor.returnValue().anyType();
            if ("GtkWidget*".equals(type.cType())) {
                if ("Gtk.Widget".equals(type.name())
                        || ("Gtk".equals(ns.name()) && "Widget".equals(type.name()))) {
                    String className = ctor.parent().name();
                    type.setAttr("name", className);
                    type.setAttr("c:type", "Gtk" + className + "*");
                }
            }
        }

        if (!"Gtk".equals(ns.name()))
            return;

        // Add javadoc to GtkTreeListModel constructor and getItem(int), with
        // guidance to handle unexpected ClassCastExceptions.
        var treeListModel = (Class) ns.select("TreeListModel").getFirst();
        inject(treeListModel, """
            ///
            /// Get the item at `position.`
            ///
            /// If `position` is greater than the number of items in `list,` `null` is
            /// returned.
            ///
            /// `null` is never returned for an index that is smaller than the length
            /// of the list.
            ///
            /// If this method throws a `ClassCastException`, check if the `passtrough`
            /// property has been set correctly.
            ///
            /// @param position the position of the item to fetch
            /// @return the object at `position.`
            /// @throws ClassCastException when the generic type of the TreeListModel
            ///   does not match the type of the returned item. This can occur when the
            ///   `passthrough` property was set to `false`. To prevent the exception,
            ///   set `passthrough` to `true` or change the generic type of the
            ///   TreeListModel to `TreeListRow`, or a superclass such as `GObject`.
            ///
            @Override
            public T getItem(int position) {
                return $T.super.getItem(position);
            }
            """, ClassNames.G_LIST_MODEL);

        var newDoc = (Doc) treeListModel.select("new", "*#doc").getFirst();
        newDoc.text = """
               Creates a new empty `GtkTreeListModel` displaying @root
               with all rows collapsed.
               
               When @passtrough is set to `false`, the generic type of the
               `GtkTreeListModel` must be (a supertype of) `TreeListRow`, to
               avoid a ClassCastException in [method@Gio.ListModel.get_item].""";
    }
}
