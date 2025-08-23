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

package org.javagi.generators;

import com.squareup.javapoet.MethodSpec;
import org.javagi.configuration.ClassNames;
import org.javagi.gir.*;
import org.javagi.util.PartialStatement;
import org.javagi.gir.Class;
import org.javagi.gir.Record;

import javax.lang.model.element.Modifier;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;
import java.util.List;

import static org.javagi.util.Conversions.*;

public class MemoryLayoutGenerator {

    private int size = 0;
    private int alignment = 0;

    // Check if a memory layout can be generated for this type.
    // Opaque structs have unknown memory layout.
    public boolean canGenerate(FieldContainer rt) {
        boolean isOpaque = switch (rt) {
            case Class c -> c.hasOpaqueStructFields() || c.opaque();
            case Record r -> r.hasOpaqueStructFields() || r.opaque();
            case Union u -> u.hasOpaqueStructFields() || u.opaque();
            default -> false;
        };
        return !isOpaque;
    }

    MethodSpec generateMemoryLayout(FieldContainer fc) {
        if (!canGenerate(fc))
            return null;

        // Ensure that size and alignment are reset
        size = 0;
        alignment = 0;

        boolean hasLongFields = fc.deepMatch(
                n -> n instanceof Type t && t.isLong(), Callback.class);

        var method = MethodSpec.methodBuilder("getMemoryLayout")
                .addJavadoc("The memory layout of the native struct.\n")
                .addJavadoc("@return the memory layout\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(MemoryLayout.class);

        // When there are `long` fields, generate 32-bit and 64-bit layout
        if (hasLongFields) {
            method.beginControlFlow("if ($T.longAsInt())", ClassNames.INTEROP);
            var layout = generateGroupLayout(fc, true);
            addReturn(method, layout);
            method.nextControlFlow("else");
            // Reset size and alignment
            size = 0;
            alignment = 0;
            layout = generateGroupLayout(fc, false);
            addReturn(method, layout);
            method.endControlFlow();
        } else {
            var layout = generateGroupLayout(fc, false);
            addReturn(method, layout);
        }

        return method.build();
    }

    private void addReturn(MethodSpec.Builder method, PartialStatement layout) {
        String code = "return " + layout.format() + ";\n";
        method.addNamedCode(code, layout.arguments());
    }

    private PartialStatement generateGroupLayout(FieldContainer fc,
                                                  boolean longAsInt) {
        var isUnion = fc instanceof Union;
        var stmt = PartialStatement.of("$memoryLayout:T.")
                .add(isUnion ? "union" : "struct")
                .add("Layout(\n$>")
                .add(generateLayouts(fc.children(), !isUnion, longAsInt))
                .add("$<\n)");
        if (fc.cType() != null)
            stmt.add(".withName(\"" + fc.cType() + "\")");
        return stmt;
    }

    private PartialStatement generateLayouts(List<Node> nodes,
                                                  boolean insertPadding,
                                                  boolean longAsInt) {
        var stmt = PartialStatement.of(null,
                "memoryLayout", MemoryLayout.class,
                "valueLayout", ValueLayout.class
        );
        int bitfieldPosition = 0;
        int initialSize = size;

        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);

            if (node instanceof FieldContainer fc) {
                if (size > initialSize) stmt.add(",\n");
                stmt.add(generateGroupLayout(fc, longAsInt));
                continue;
            }

            if (! (node instanceof Field field)) {
                continue;
            }

            // Get the size of the field, in bytes
            int s = field.getSize(longAsInt);

            // Update alignment
            if (s > alignment && s <= 8)
                alignment = s;

            // Bitfield?
            int bits = field.bits();
            if (bits > 0) {
                bitfieldPosition += bits;
                boolean last = (i + 1 == nodes.size())
                        || (! (nodes.get(i + 1) instanceof Field))
                        || (nodes.get(i + 1) instanceof Field f
                                && f.bits() == -1);

                if (last || bitfieldPosition > (s * 8)) {
                    if (size > initialSize) stmt.add(",\n");
                    size += s;
                    stmt.add("$memoryLayout:T.sequenceLayout(" + s + ", $valueLayout:T.JAVA_BYTE)")
                        .add(" /* bitfield */");
                    bitfieldPosition = last ? 0 : bits;
                }
                continue;
            }

            if (size > initialSize) stmt.add(",\n");

            // Calculate padding
            if (insertPadding) {
                // If the previous field had a smaller byte-size than this one,
                // add padding (to a maximum of 8 bytes)
                if (size % s % 8 > 0) {
                    int padding = (s - (size % s)) % 8;
                    stmt.add("$memoryLayout:T.paddingLayout(" + padding + ")")
                        .add(",\n");
                    size += padding;
                }
            }

            // Write the memory layout declaration
            stmt.add(getFieldLayout(field, longAsInt));
            if (field.name() != null)
                stmt.add(".withName(\"" + field.name() + "\")");

            size += s;
        }

        // Add trailing padding, needed to allocate arrays
        if (insertPadding && alignment > 0) {
            int padding = (alignment - (size % alignment)) % alignment;
            if (padding > 0) {
                stmt.add(",\n$memoryLayout:T.paddingLayout(" + padding + ")");
                size += padding;
            }
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
        RegisteredType target = type.lookup();

        // Recursive lookup for aliases
        if (target instanceof Alias alias && alias.anyType() instanceof Type t)
            return layoutForType(t, longAsInt);

        // Proxy objects with a known memory layout
        if (!type.isPointer()
                && target instanceof FieldContainer fc
                && canGenerate(fc)) {
            String tag = type.toTypeTag();
            return PartialStatement.of("$" + tag + ":T.getMemoryLayout()",
                    tag, type.typeName());
        }

        // Plain value layout
        return getValueLayout(type, longAsInt);
    }
}
