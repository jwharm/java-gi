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

public final class Union
        extends Multiplatform
        implements StandardLayoutType, FieldContainer {

    @Override
    public Namespace parent() {
        return (Namespace) super.parent();
    }

    public Union(Map<String, String> attributes,
                 List<Node> children,
                 int platforms) {
        super(attributes, children, platforms);
    }

    @Override
    public Union mergeWith(RegisteredType rt) {
        if (rt instanceof Union other)
            return new Union(
                    attributes(),
                    union(children(), other.children()),
                    platforms() | other.platforms());
        return this;
    }

    public boolean isOpaque() {
        return fields().isEmpty() && records().isEmpty();
    }

    public List<Field> fields() {
        return filter(children(), Field.class);
    }

    public List<Function> functions() {
        return filter(children(), Function.class);
    }

    public List<Constructor> constructors() {
        return filter(children(), Constructor.class);
    }

    public List<Method> methods() {
        return filter(children(), Method.class);
    }

    public List<Record> records() {
        return filter(children(), Record.class);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || obj.getClass() != this.getClass())
            return false;

        var that = (Union) obj;
        return Objects.equals(this.name(), that.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name());
    }
}
