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

import org.javagi.configuration.ClassNames;
import org.javagi.util.PartialStatement;

import static org.javagi.util.CollectionUtils.*;
import static java.util.Collections.emptyList;

import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Boxed extends GirElement implements StandardLayoutType {

    public Boxed(Map<String, String> attributes, List<Node> children) {
        super(attributes, children);
    }

    @Override
    public String name() {
        return attr("glib:name");
    }

    @Override
    public Namespace parent() {
        return (Namespace) super.parent();
    }

    @Override
    public Boxed mergeWith(RegisteredType rt) {
        if (rt instanceof Boxed other)
            return new Boxed(attributes(), union(children(), other.children()));
        return this;
    }

    @Override
    public PartialStatement destructorName() {
        var tag = typeTag();
        return PartialStatement.of("(_b -> $gobjects:T.boxedFree($" + tag + ":T.getType(), _b == null ? $memorySegment:T.NULL : _b.handle()))",
                tag, typeName(),
                "gobjects", ClassNames.G_OBJECTS,
                "memorySegment", MemorySegment.class);
    }

    @Override
    public List<Field> fields() {
        return emptyList();
    }

    @Override
    public boolean opaque() {
        return true;
    }

    public List<Function> functions() {
        return filter(children(), Function.class);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || obj.getClass() != this.getClass())
            return false;

        var that = (Boxed) obj;
        return Objects.equals(this.name(), that.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name());
    }
}
