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

public abstract sealed class TypedValue extends GirElement
        permits Constant, Field, InstanceParameter, Parameter, Property, ReturnValue {

    public TypedValue(Map<String, String> attributes, List<GirElement> children) {
        super(attributes, children);
    }

    public boolean allocatesMemory() {
        return switch(anyType()) {
            case null -> true; // callback
            case Array _ -> true;
            case Type type -> type.isActuallyAnArray() || "java.lang.String".equals(type.javaType());
        };
    }

    public String name() {
        return attr("name");
    }

    public InfoElements infoElements() {
        return super.infoElements();
    }

    public AnyType anyType() {
        return findAny(children(), AnyType.class);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TypedValue) obj;
        return  Objects.equals(this.name(), that.name()) &&
                Objects.equals(this.anyType(), that.anyType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name(), anyType());
    }
}
