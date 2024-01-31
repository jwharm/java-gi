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
     * Change the "name" attribute of the element to the provided new name
     * @param elem element to rename
     * @param newName the new name
     * @return the renamed element
     * @param <T> the element must be a GirElement
     */
    default <T extends GirElement> T rename(T elem, String newName) {
        return changeAttribute(elem, "name", newName);
    }

    /**
     * Change an attribute with the provided name to the provided value
     * @param elem the GirElement for which to update the attribute
     * @param attrName the name of the attribute
     * @param newValue the new value for the attribute
     * @return a new instance of the same type as {@code elem}, with the new attribute value
     * @param <T> the element must be a GirElement
     */
    @SuppressWarnings("unchecked")
    default <T extends GirElement> T changeAttribute(T elem, String attrName, String newValue) {
        var newAttrs = new HashMap<>(elem.attributes());
        newAttrs.put(attrName, newValue);
        return (T) switch(elem) {
            case Alias a -> new Alias(newAttrs, a.children(), a.platforms());
            case Array a -> new Array(newAttrs, a.children());
            case Attribute _ -> new Attribute(newAttrs);
            case Bitfield b -> new Bitfield(newAttrs, b.children(), b.platforms());
            case Boxed b -> new Boxed(newAttrs, b.children(), b.platforms());
            case Callback c -> new Callback(newAttrs, c.children(), c.platforms());
            case CInclude c -> new CInclude(newAttrs);
            case Class c -> new Class(newAttrs, c.children(), c.platforms());
            case Constant c -> new Constant(newAttrs, c.children(), c.platforms());
            case Constructor c -> new Constructor(newAttrs, c.children(), c.platforms());
            case Doc d -> new Doc(newAttrs, d.text());
            case Docsection d -> new Docsection(newAttrs, d.children());
            case DocDeprecated d -> d;
            case DocVersion d -> d;
            case Enumeration e -> new Enumeration(newAttrs, e.children(), e.platforms());
            case Field f -> new Field(newAttrs, f.children());
            case Function f -> new Function(newAttrs, f.children(), f.platforms());
            case FunctionMacro _ -> new FunctionMacro();
            case Implements _ -> new Implements(newAttrs);
            case Include _ -> new Include(newAttrs);
            case InstanceParameter i -> new InstanceParameter(newAttrs, i.children());
            case Interface i -> new Interface(newAttrs, i.children(), i.platforms());
            case Member m -> new Member(newAttrs, m.children());
            case Method m -> new Method(newAttrs, m.children(), m.platforms());
            case Namespace n -> new Namespace(newAttrs, n.children(), n.platforms());
            case Package _ -> new Package(newAttrs);
            case Parameter p -> new Parameter(newAttrs, p.children());
            case Parameters p -> p;
            case Prerequisite _ -> new Prerequisite(newAttrs);
            case Property p -> new Property(newAttrs, p.children(), p.platforms());
            case Record r -> new Record(newAttrs, r.children(), r.platforms());
            case Repository r -> new Repository(newAttrs, r.children());
            case ReturnValue r -> new ReturnValue(newAttrs, r.children());
            case Signal s -> new Signal(newAttrs, s.children(), s.platforms());
            case SourcePosition _ -> new SourcePosition(newAttrs);
            case Type t -> new Type(newAttrs, t.children());
            case Union u -> new Union(newAttrs, u.children(), u.platforms());
            case Varargs v -> v;
            case VirtualMethod v -> new VirtualMethod(newAttrs, v.children(), v.platforms());
            default -> throw new UnsupportedOperationException("Unsupported element: " + elem);
        };
    }
}
