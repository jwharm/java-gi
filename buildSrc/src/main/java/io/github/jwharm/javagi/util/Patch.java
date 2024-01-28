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
import io.github.jwharm.javagi.gir.Record;

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
     * Remove the type with the provided name from the namespace.
     *
     * @param ns   the namespace to remove the type from
     * @param name the name of the type
     * @return the namespace with the type removed
     */
    default Namespace removeType(Namespace ns, String name) {
        List<GirElement> children = ns.children().stream()
                .filter(node -> !(node instanceof RegisteredType type && name.equals(type.name())))
                .toList();
        return new Namespace(ns.attributes(), children, ns.platforms());
    }

    /**
     * Change an attribute with the provided name to the provided value
     * @param rt the RegisteredType for which to update the attribute
     * @param attrName the name of the attribute
     * @param newValue the new value for the attribute
     * @return a new instance of the same type as {@code rt}, with the new attribute value
     */
    default RegisteredType changeAttribute(RegisteredType rt, String attrName, String newValue) {
        var newAttrs = new HashMap<>(rt.attributes());
        newAttrs.put(attrName, newValue);
        return switch(rt) {
            case Alias a -> new Alias(newAttrs, a.children(), a.platforms());
            case Bitfield b -> new Bitfield(newAttrs, b.children(), b.platforms());
            case Boxed b -> new Boxed(newAttrs, b.children(), b.platforms());
            case Callback c -> new Callback(newAttrs, c.children(), c.platforms());
            case Class c -> new Class(newAttrs, c.children(), c.platforms());
            case Enumeration e -> new Enumeration(newAttrs, e.children(), e.platforms());
            case Interface i -> new Interface(newAttrs, i.children(), i.platforms());
            case Record r -> new Record(newAttrs, r.children(), r.platforms());
            case Union u -> new Union(newAttrs, u.children(), u.platforms());
        };
    }
}
