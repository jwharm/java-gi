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
import java.util.Map;
import java.util.Objects;

public final class Constructor extends GirElement implements Callable {

    public Constructor(Map<String, String> attributes, List<Node> children) {
        super(attributes, children);
    }

    @Override
    public RegisteredType parent() {
        return (RegisteredType) super.parent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Constructor other = (Constructor) o;
        return Objects.equals(callableAttrs().cIdentifier(),
                              other.callableAttrs().cIdentifier());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), callableAttrs().cIdentifier());
    }
}
