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

import com.squareup.javapoet.TypeName;

import java.lang.foreign.MemorySegment;

import static io.github.jwharm.javagi.util.Conversions.*;

/**
 * This interface is implemented by GIR elements whose name refers to a RegisteredType.
 * The {@link #get()} method will retrieve the type from the GIR model.
 */
public interface TypeReference {
    Namespace namespace();
    String name();

    default RegisteredType get() {
        String name = name();
        if (name == null)
            return null;

        int dot = name.indexOf('.');
        if (dot == -1)
            return namespace().registeredTypes().get(name);

        return namespace().parent()
                .lookupNamespace(name.substring(0, dot))
                .registeredTypes().get(name.substring(dot + 1));
    }

    default TypeName typeName() {
        // Type without name: Unknown, fallback to MemorySegment
        if (name() == null)
            return TypeName.get(MemorySegment.class);

        var type = get();
        // A typeclass or typeinterface is an inner class
        if (type instanceof Record rec) {
            var outer = rec.isGTypeStructFor();
            if (outer != null)
                return outer.typeName().nestedClass(
                        toJavaSimpleType(type.name(), type.namespace()));
        }
        return toJavaQualifiedType(type.name(), type.namespace());
    }

    default String javaType() {
        return typeName().toString();
    }

    static RegisteredType get(Namespace namespace, String name) {
        if (namespace == null || name == null)
            return null;

        return new TypeReference() {
            public Namespace namespace() { return namespace; }
            public String    name()      { return name;      }
        }.get();
    }
}
