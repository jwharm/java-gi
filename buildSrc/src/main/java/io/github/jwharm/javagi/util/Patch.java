/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 the Java-GI developers
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

package io.github.jwharm.javagi.util;

import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.gir.Package;
import io.github.jwharm.javagi.gir.Record;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Interface for patches to apply to the GIR model.
 */
public interface Patch {

    /**
     * Apply a patch to the GIR model
     * @param element a newly generated GIR element
     * @return the patched GIR element
     */
    GirElement patch(GirElement element);

    /**
     * Remove the types with the provided names from the namespace.
     *
     * @param ns    the namespace to remove the type from
     * @param names the name of the types
     * @return      the namespace with the type removed
     */
    default Namespace removeType(Namespace ns, String... names) {
        List<String> list = Arrays.asList(names);
        List<GirElement> children = ns.children().stream()
                .filter(node -> !(node instanceof RegisteredType type
                        && list.contains(type.name())))
                .toList();
        return ns.withChildren(children);
    }

    /**
     * Remove the function with the provided name from the namespace.
     *
     * @param ns   the namespace to remove the function from
     * @param name the name of the function
     * @return     the namespace with the function removed
     */
    default Namespace removeFunction(Namespace ns, String name) {
        return remove(ns, Function.class, "name", name);
    }

    /**
     * Remove the child elements with the provided attribute.
     *
     * @param elem  the element to remove the child element from
     * @param key   the attribute key
     * @param value the attribute value
     * @return the element with the child element removed
     * @param <T> the element must be a GirElement
     */
    default <T extends GirElement> T remove(T elem,
                                            java.lang.Class<? extends GirElement> type,
                                            String key,
                                            String value) {
        List<GirElement> children = elem.children().stream()
                .filter(node -> !(type.isInstance(node)
                        && value.equals(node.attributes().get(key))))
                .toList();
        return elem.withChildren(children);
    }

    /**
     * Change the "name" attribute of the element to the provided new name
     * @param elem element to rename
     * @param newName the new name
     * @return the renamed element
     * @param <T> the element must be a GirElement
     */
    default <T extends GirElement> T rename(T elem, String newName) {
        return elem.withAttribute("name", newName);
    }
}
