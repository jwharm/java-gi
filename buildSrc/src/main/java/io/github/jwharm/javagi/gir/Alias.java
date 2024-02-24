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
import static io.github.jwharm.javagi.util.Conversions.toJavaBaseType;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Alias extends Multiplatform implements RegisteredType {

    public Alias(Map<String, String> attributes, List<Node> children, int platforms) {
        super(attributes, children, platforms);
    }

    @Override
    public Namespace parent() {
        return (Namespace) super.parent();
    }

    @Override
    public String getTypeFunc() {
        if (type().isPrimitive()) return null;
        return type().get().getTypeFunc();
    }

    @Override
    public String constructorName() {
        RegisteredType target = type().get();
        return target == null ? null : target.constructorName();
    }

    @Override
    public String getInteropString(String paramName, boolean isPointer, Scope scope) {
        RegisteredType target = type().get();
        if (target != null)
            return target.getInteropString(paramName, isPointer, scope);

        return switch(toJavaBaseType(type().name())) {
            case "String", "MemorySegment" -> paramName + ".getValue()";
            default -> paramName + ".getValue()." + type().typeName() + "Value()";
        };
    }

    @Override
    public Alias mergeWith(RegisteredType rt) {
        if (rt instanceof Alias other)
            return new Alias(
                    attributes(),
                    union(children(), other.children()),
                    platforms() | other.platforms());
        return this;
    }

    public Type type() {
        return findAny(children(), Type.class);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || obj.getClass() != this.getClass())
            return false;

        var that = (Alias) obj;
        return Objects.equals(this.name(), that.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name());
    }
}
