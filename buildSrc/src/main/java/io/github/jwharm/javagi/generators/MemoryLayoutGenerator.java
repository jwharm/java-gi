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
import io.github.jwharm.javagi.configuration.ClassNames;
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

    // Check if a memory layout can be generated for this type
    private boolean canGenerate(RegisteredType rt) {
        if (! (rt instanceof FieldContainer))
            return false;

        // Opaque structs have unknown memory layout
        boolean isOpaque = switch (rt) {
            case Class c -> c.hasOpaqueStructFields() || c.isOpaque();
            case Record r -> r.hasOpaqueStructFields() || r.isOpaque();
            case Union u -> u.hasOpaqueStructFields() || u.isOpaque();
            default -> false;
        };
        return !isOpaque;
    }

    MethodSpec generateMemoryLayout(RegisteredType rt) {
        if (!canGenerate(rt))
            return null;

        var fieldList = CollectionUtils.filter(rt.children(), Field.class);
        var unionList = CollectionUtils.filter(rt.children(), Union.class);
        if (fieldList.isEmpty()
                && !unionList.isEmpty()
                && !unionList.getFirst().fields().isEmpty())
            fieldList = unionList.getFirst().fields();

        boolean isUnion = rt instanceof Union || !unionList.isEmpty();

        boolean hasLongFields = rt.deepMatch(
                n -> n instanceof Type t && t.isLong(), Callback.class);

        var method = MethodSpec.methodBuilder("getMemoryLayout")
                .addJavadoc("The memory layout of the native struct.\n")
                .addJavadoc("@return the memory layout\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(MemoryLayout.class);

        // When there are `long` fields, generate 32-bit and 64-bit layout
        if (hasLongFields) {
            method.beginControlFlow("if ($T.longAsInt())", ClassNames.INTEROP);
            var layout = generateMemoryLayout(rt.cType(), fieldList, isUnion, true);
            method.addNamedCode(layout.format(), layout.arguments())
                  .nextControlFlow("else");
            layout = generateMemoryLayout(rt.cType(), fieldList, isUnion, false);
            method.addNamedCode(layout.format(), layout.arguments())
                  .endControlFlow();
        } else {
            var layout = generateMemoryLayout(rt.cType(), fieldList, isUnion, false);
            method.addNamedCode(layout.format(), layout.arguments());
        }

        return method.build();
    }

    private PartialStatement generateMemoryLayout(String name,
                                                  List<Field> fieldList,
                                                  boolean isUnion,
                                                  boolean longAsInt) {
        // The $> and $< in the statement increase and decrease indentation
        return PartialStatement.of("return $memoryLayout:T."
                        + (isUnion ? "union" : "struct") + "Layout(\n$>")
                .add(generateFieldLayouts(fieldList, isUnion, longAsInt))
                .add("$<\n).withName(\"" + name + "\");\n");
    }

    private PartialStatement generateFieldLayouts(List<Field> fieldList,
                                                  boolean isUnion,
                                                  boolean longAsInt) {
        var stmt = PartialStatement.of(null,
                "memoryLayout", MemoryLayout.class,
                "valueLayout", ValueLayout.class
        );
        int size = 0;
        for (Field field : fieldList) {
            if (size > 0) stmt.add(",\n");

            // Get the byte size of the field, in bytes
            int s = field.getSize(longAsInt);

            // Calculate padding (except for union layouts)
            if (!isUnion) {
                // If the previous field had a smaller byte-size than this one,
                // add padding (to a maximum of 8 bytes)
                if (size % s % 8 > 0) {
                    int padding = (s - (size % s)) % 8;
                    stmt.add("$memoryLayout:T.paddingLayout(" + padding + "),\n");
                    size += padding;
                }
            }

            // Write the memory layout declaration
            stmt.add(getFieldLayout(field, longAsInt))
                    .add(".withName(\"" + field.name() + "\")");
            size += s;
        }
        return stmt;
    }

    private PartialStatement getFieldLayout(Field f, boolean longAsInt) {
        return switch (f.anyType()) {
            case null -> PartialStatement.of("$valueLayout:T.ADDRESS"); // callback
            case Type type -> layoutForType(type, longAsInt);
            case Array array -> {
                if (array.fixedSize() > 0) {
                    var type = (Type) array.anyType();
                    yield PartialStatement.of("$memoryLayout:T.sequenceLayout("
                                    + array.fixedSize() + ", ")
                            .add(layoutForType(type, longAsInt))
                            .add(")");
                } else {
                    yield PartialStatement.of("$valueLayout:T.ADDRESS");
                }
            }
        };
    }

    private PartialStatement layoutForType(Type type, boolean longAsInt) {
        RegisteredType target = type.get();

        // Recursive lookup for aliases
        if (target instanceof Alias alias)
            return layoutForType(alias.type(), longAsInt);

        // Proxy objects with a known memory layout
        if (!type.isPointer() && canGenerate(target)) {
            String tag = type.toTypeTag();
            return PartialStatement.of("$" + tag + ":T.getMemoryLayout()",
                    tag, type.typeName());
        }

        // Plain value layout
        String layout = getValueLayout(type, longAsInt);
        return PartialStatement.of("$valueLayout:T." + layout);
    }
}
