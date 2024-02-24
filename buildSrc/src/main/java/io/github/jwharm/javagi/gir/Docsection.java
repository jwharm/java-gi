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

package io.github.jwharm.javagi.gir;

import static io.github.jwharm.javagi.util.CollectionUtils.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Docsection extends GirElement {

    public Docsection(Map<String, String> attributes, List<Node>children) {
        super(attributes, children);
    }

    public String name() {
        return attr("name");
    }

    public Doc doc() {
        return findAny(children(), Doc.class);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Docsection) obj;
        return Objects.equals(this.name(), that.name()) &&
                Objects.equals(this.doc(), that.doc());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name(), doc());
    }
}
