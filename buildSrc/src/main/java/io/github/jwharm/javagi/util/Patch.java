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

import java.util.ArrayList;
import java.util.List;

/**
 * Interface for patches to apply to the GIR model.
 */
public interface Patch {

    /**
     * Apply a patch to the GIR model.
     *
     * @param  element   a newly generated GIR element
     * @param  namespace the name of the namespace of the GIR element
     * @return the patched GIR element
     */
    GirElement patch(GirElement element, String namespace);

    /**
     * Remove the child elements with the provided attribute.
     *
     * @param elem  the element to remove the child element from
     * @param type  the type of the element to remove
     * @param key   the attribute key
     * @param value the attribute value
     * @return the element with the child element removed
     * @param <T> the element must be a GirElement
     */
    default <T extends GirElement> T remove(T elem,
                                            java.lang.Class<? extends GirElement> type,
                                            String key,
                                            String value) {
        List<Node> children = elem.children().stream()
                .filter(node -> !(type.isInstance(node)
                        && value.equals(node.attributes().get(key))))
                .toList();
        return elem.withChildren(children);
    }

    /**
     * Return a copy of the parent element in which the child has been added.
     *
     * @param  parent the parent element
     * @param  child  the element to add to the parent
     * @return a copy of parent in which the child has been added
     * @param <T> both elements must be a GirElement
     */
    default <T extends GirElement> T add(T parent, T child) {
        var mutableList = new ArrayList<>(parent.children());
        mutableList.add(child);
        return parent.withChildren(mutableList);
    }
}
