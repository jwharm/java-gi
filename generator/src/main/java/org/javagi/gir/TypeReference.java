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

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.TypeName;

import java.lang.foreign.MemorySegment;

import static org.javagi.util.Conversions.*;

/**
 * This interface is implemented by GIR elements whose name refers to a
 * RegisteredType. The {@link #lookup()} method will retrieve the type from the
 * GIR model.
 */
public interface TypeReference {
    Namespace namespace();
    String name();

    static RegisteredType lookup(Namespace namespace, String name) {
        if (name == null || namespace == null)
            return null;

        int dot = name.indexOf('.');
        if (dot == -1)
            return namespace.registeredTypes().get(name);

        return namespace.parent()
                .lookupNamespace(name.substring(0, dot))
                .registeredTypes().get(name.substring(dot + 1));
    }

    default RegisteredType lookup() {
        return lookup(namespace(), name());
    }

    default TypeName typeName() {
        // Type without name: Unknown, fallback to MemorySegment
        if (name() == null)
            return TypeName.get(MemorySegment.class);

        // Get the target type
        var type = lookup();

        if (type instanceof Record rec) {
            // GBytes is treated as a byte[]
            if (rec.checkIsGBytes())
                return ArrayTypeName.of(byte.class);

            // GString is treated as a String
            if (rec.checkIsGString())
                return TypeName.get(String.class);

            // A TypeClass or TypeInterface is an inner class
            var outer = rec.isGTypeStructFor();
            if (outer != null)
                return outer.typeName().nestedClass(
                        toJavaSimpleType(type.name(), type.namespace()));
        }

        // Target not found: fallback to MemorySegment
        if (type == null) {
            System.err.println("Cannot resolve type " + name());
            return TypeName.get(MemorySegment.class);
        }

        return toJavaQualifiedType(type.name(), type.namespace());
    }

    default String javaType() {
        return typeName().toString();
    }
}
