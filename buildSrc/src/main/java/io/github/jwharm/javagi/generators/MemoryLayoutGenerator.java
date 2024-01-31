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

package io.github.jwharm.javagi.generators;

import com.squareup.javapoet.MethodSpec;
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.CollectionUtils;
import io.github.jwharm.javagi.util.PartialStatement;

import javax.lang.model.element.Modifier;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;
import java.util.List;

import static io.github.jwharm.javagi.util.Conversions.*;

public class MemoryLayoutGenerator {

    MethodSpec generateMemoryLayout(RegisteredType rt) {
        if (! (rt instanceof FieldContainer))
            return null;

        // Opaque structs have unknown memory layout and should not have an allocator
        if (rt instanceof Class c  && (c.hasOpaqueStructFields() || c.isOpaque())) return null;
        if (rt instanceof Record r && (r.hasOpaqueStructFields() || r.isOpaque())) return null;
        if (rt instanceof Union u  && (u.hasOpaqueStructFields() || u.isOpaque())) return null;

        var fieldList = CollectionUtils.filter(rt.children(), Field.class);
        var unionList = CollectionUtils.filter(rt.children(), Union.class);
        if (fieldList.isEmpty() && !unionList.isEmpty() && !unionList.getFirst().fields().isEmpty())
            fieldList = unionList.getFirst().fields();

        boolean isUnion = rt instanceof Union || !unionList.isEmpty();
        PartialStatement fieldLayouts = generateFieldLayouts(fieldList, isUnion);
        PartialStatement layout = PartialStatement.of("return $memoryLayout:T." + (isUnion ? "union" : "struct") + "Layout(\n$>")
                .add(fieldLayouts)
                .add("$<\n).withName(\"" + rt.cType() + "\");\n");

        return MethodSpec.methodBuilder("getMemoryLayout")
                .addJavadoc("The memory layout of the native struct.\n")
                .addJavadoc("@return the memory layout\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(MemoryLayout.class)
                .addNamedCode(layout.format(), layout.arguments())
                .build();
    }

    private PartialStatement generateFieldLayouts(List<Field> fieldList, boolean isUnion) {
        PartialStatement stmt = PartialStatement.of(null, "memoryLayout", MemoryLayout.class);
        int size = 0;
        for (Field field : fieldList) {
            if (size > 0) stmt.add(",\n");

            // Get the byte size of the field. For example: int = 4 bytes, pointer = 8 bytes, char = 1 byte
            int s = field.getSize();

            // Calculate padding (except for union layouts)
            if (!isUnion) {
                // If the previous field had a smaller byte size than this one, add padding (to a maximum of 8 bytes)
                if (size % s % 8 > 0) {
                    int padding = (s - (size % s)) % 8;
                    stmt.add("$memoryLayout:T.paddingLayout(" + padding + "),\n");
                    size += padding;
                }
            }

            // Write the memory layout declaration
            stmt.add(getFieldLayout(field));
            size += s;
        }
        return stmt;
    }

    private PartialStatement getFieldLayout(Field f) {
        if (f.callback() != null)
            return PartialStatement.of("$valueLayout:T.ADDRESS.withName(\"" + f.name() + "\")",
                    "valueLayout", ValueLayout.class);

        return switch (f.anyType()) {
            case Type type -> {
                RegisteredType target = type.get();

                // Pointers, strings and callbacks are memory addresses
                if (type.isPointer()
                        || "java.lang.String".equals(type.javaType())
                        || "java.lang.foreign.MemorySegment".equals(type.javaType())
                        || target instanceof Callback)
                    yield PartialStatement.of("$valueLayout:T.ADDRESS.withName(\"" + f.name() + "\")",
                            "valueLayout", ValueLayout.class);

                // Bitfields and enumerations are integers
                if (target instanceof FlaggedType)
                    yield PartialStatement.of("$valueLayout:T.JAVA_INT.withName(\"" + f.name() + "\")",
                            "valueLayout", ValueLayout.class);

                // Primitive types and aliases
                if (type.isPrimitive() || (target instanceof Alias a && a.type().isPrimitive()))
                    yield PartialStatement.of("$valueLayout:T." + getValueLayout(type) + ".withName(\"" + f.name() + "\")",
                            "valueLayout", ValueLayout.class);

                // Opaque type (with unknown memory layout)
                if (target instanceof Record rec && rec.isOpaque())
                    yield PartialStatement.of("$valueLayout:T.ADDRESS.withName(\"" + f.name() + "\")",
                            "valueLayout", ValueLayout.class);

                // For Proxy objects we recursively get the memory layout
                else {
                    String classNameTag = type.toTypeTag();
                    yield PartialStatement.of("$" + classNameTag + ":T.getMemoryLayout().withName(\"" + f.name() + "\")",
                            classNameTag, type.typeName());
                }
            }
            case Array array -> {
                if (array.fixedSize() > 0) {
                    Type type = (Type) array.anyType();
                    RegisteredType target = type.get();
                    if (target != null) {
                        String classNameTag = type.toTypeTag();
                        yield PartialStatement.of("$memoryLayout:T.sequenceLayout(" + array.fixedSize() + ", $" + classNameTag + ":T.getMemoryLayout()).withName(\"" + f.name() + "\")",
                                "memoryLayout", MemoryLayout.class,
                                classNameTag, target.typeName());
                    } else {
                        yield PartialStatement.of("$memoryLayout:T.sequenceLayout(" + array.fixedSize() + ", $valueLayout:T." + getValueLayout(type) + ").withName(\"" + f.name() + "\")",
                                "memoryLayout", MemoryLayout.class,
                                "valueLayout", ValueLayout.class);
                    }
                } else {
                    yield PartialStatement.of("$valueLayout:T.ADDRESS.withName(\"" + f.name() + "\")",
                            "valueLayout", ValueLayout.class);
                }
            }
        };
    }

}
