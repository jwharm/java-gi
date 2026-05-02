/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2026 the Java-GI developers
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

import org.javagi.javapoet.CodeBlock;
import org.javagi.javapoet.MethodSpec;
import org.javagi.configuration.ClassNames;
import org.javagi.gir.*;
import org.javagi.util.Conversions;
import org.javagi.gir.Class;
import org.javagi.gir.Record;

import javax.lang.model.element.Modifier;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;
import java.util.List;
import java.util.Objects;

import static java.util.function.Predicate.not;

public class MemoryLayoutGenerator {

    public final static String UNKNOWN_LAYOUT = "javagi$unknown";

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

    /**
     * A small custom data structure to model memory layouts.
     * It is a tree of Layouts objects, where each Layout is either a struct,
     * union or member. It's a bit easier to work with then the raw gir data.
     */
    private sealed interface Layout permits LStruct, LUnion, LMember {
        String name();
        int alignment();
        int size();
        CodeBlock generateMemoryLayout();

        static Layout of(boolean longAsInt, FieldContainer fc) {
            String name = fc.cType() != null ? fc.cType() : fc.name();
            List<Layout> members = fc.children().stream()
                    .map(node ->
                            switch (node) {
                                case Field f -> new LMember(longAsInt, f);
                                case FieldContainer nested -> of(longAsInt, nested);
                                default -> null;
                            })
                    .filter(not(Objects::isNull))
                    .toList();

            if (fc instanceof Union)
                return new LUnion(longAsInt, name, members);
            else
                return new LStruct(longAsInt, name, members);
        }
    }

    private record LStruct(boolean longAsInt, String name, List<Layout> members) implements Layout {
        public int alignment() {
            return members.stream().mapToInt(Layout::alignment).max().orElse(0);
        }

        public int size() {
            return members.stream().mapToInt(Layout::size).sum();
        }

        public CodeBlock generateMemoryLayout() {
            var stmt = CodeBlock.builder().add("$T.structLayout($>\n", MemoryLayout.class);
            int pos = 0;
            int bits = 0;
            boolean first = true;

            for (var member : members()) {
                // Bitfields
                if (member instanceof LMember m && m.bits() > 0) {
                    boolean generate = bits == 0; // generate first
                    bits += m.bits();
                    int size = m.size() * 8;
                    if (bits > size) {
                        bits = bits % size;
                        generate = true; // generate on overflow
                    }
                    if (!generate) continue;
                } else if (bits > 0) {
                    bits = 0;
                }

                if (first) first = false; else stmt.add(",\n");

                // Insert padding
                int unaligned = pos % member.alignment();
                if (unaligned != 0) {
                    int padding = member.alignment() - unaligned;
                    stmt.add("$T.paddingLayout($L),\n", MemoryLayout.class, padding);
                    pos += padding;
                }

                stmt.add(member.generateMemoryLayout());
                pos += member.size();
            }

            // Add trailing padding
            int unaligned = pos % alignment();
            if (unaligned != 0) {
                int padding = alignment() - unaligned;
                stmt.add(",\n$T.paddingLayout($L)", MemoryLayout.class, padding);
            }

            stmt.add("$<\n)");
            if (name != null)
                stmt.add(".withName($S)", name);

            return stmt.build();
        }
    }

    private record LUnion(boolean longAsInt, String name, List<Layout> members) implements Layout {
        public int alignment() {
            return members.stream().mapToInt(Layout::alignment).max().orElse(0);
        }

        public int size() {
            return members.stream().mapToInt(Layout::size).max().orElse(0);
        }

        public CodeBlock generateMemoryLayout() {
            var stmt = CodeBlock.builder().add("$T.unionLayout($>\n", MemoryLayout.class);
            boolean first = true;

            for (var member : members()) {
                if (first) first = false; else stmt.add(",\n");
                stmt.add(member.generateMemoryLayout());
            }

            stmt.add("$<\n)");
            if (name != null)
                stmt.add(".withName($S)", name);

            return stmt.build();
        }
    }

    private record LMember(boolean longAsInt, Field field) implements Layout {
        public String name() {
            return field.name();
        }

        public int alignment() {
            return field.getAlignment(longAsInt);
        }

        public int size() {
            return field.getSize(longAsInt);
        }

        public int bits() {
            return field.bits();
        }

        public CodeBlock generateMemoryLayout() {
            var stmt = CodeBlock.builder();

            if (field.anyType() instanceof Type t)
                stmt.add(layoutForType(t));
            else if (field.anyType() instanceof Array a && a.fixedSize() > 0)
                stmt.add("$T.sequenceLayout($L, $L)",
                        MemoryLayout.class, a.fixedSize(), layoutForType((Type) a.anyType()));
            else // Array (no fixed size) or callback
                stmt.add("$T.ADDRESS", ValueLayout.class);

            if (bits() > 0)
                return stmt.add("$T.sequenceLayout($L, $T.JAVA_BYTE) /* bitfield */",
                        MemoryLayout.class, size(), ValueLayout.class).build();

            if (name() != null)
                stmt.add(".withName($S)", name());

            return stmt.build();
        }

        private CodeBlock layoutForType(Type type) {
            RegisteredType target = type.lookup();

            // Recursive lookup for aliases
            if (target instanceof Alias alias && alias.anyType() instanceof Type t)
                return layoutForType(t);

            // Proxy objects with a known memory layout
            if (!type.isPointer()
                    && target instanceof FieldContainer fc
                    && new MemoryLayoutGenerator().canGenerate(fc))
                return CodeBlock.of("$T.getMemoryLayout()", type.typeName());

            // Plain value layout
            return Conversions.getValueLayout(type, longAsInt);
        }
    }

    public MethodSpec generateMemoryLayout(FieldContainer fc) {
        var method = MethodSpec.methodBuilder("getMemoryLayout")
                .addJavadoc("The memory layout of the native struct.\n\n")
                .addJavadoc("@return the memory layout\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(MemoryLayout.class);

        if (!canGenerate(fc))
            return method.addStatement("return $T.ADDRESS.withName($S)",
                    ValueLayout.class, UNKNOWN_LAYOUT).build();

        // When there are `long` fields, generate 32-bit and 64-bit layout
        boolean hasLongFields = fc.deepMatch(n -> n instanceof Type t && t.isLong(), Callback.class);
        if (hasLongFields)
            method.beginControlFlow("if ($T.longAsInt())", ClassNames.INTEROP)
                    .addStatement("return $L", Layout.of(true, fc).generateMemoryLayout())
                    .nextControlFlow("else")
                    .addStatement("return $L", Layout.of(false, fc).generateMemoryLayout())
                    .endControlFlow();
        else
            method.addStatement("return $L", Layout.of(false, fc).generateMemoryLayout());

        return method.build();
    }
}
