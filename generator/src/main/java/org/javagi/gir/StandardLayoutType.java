/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 the Java-GI developers
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

package org.javagi.gir;

import java.util.List;

public sealed interface StandardLayoutType
        extends FieldContainer
        permits Boxed, Record, Union {

    default String cSymbolPrefix() {
        return attr("c:symbol-prefix");
    }

    private Callable lookup(String cIdentifier) {
        if (cIdentifier == null)
            return null;

        Node node = namespace().parent().lookupCIdentifier(cIdentifier);
        if (node instanceof Callable callable)
            return callable;

        return null;
    }

    default boolean isBoxedType() {
        if (getTypeFunc() == null)
            return false;

        if (cType() == null)
            return true;

        // GValue and GVariant have a get-type func, but are not boxed types
        return !List.of("GValue", "GVariant", "GClosure").contains(cType());
    }

    default Callable copyFunction() {
        return lookup(attr("copy-function"));
    }

    default Callable freeFunction() {
        return lookup(attr("free-function"));
    }
}
