/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Enumeration extends ValueWrapper {

    public Enumeration(GirElement parent, String name, String cType, String getType, String version) {
        super(parent, name, null, cType, getType, version);
    }

    public void generate(SourceWriter writer) throws IOException {
        generateCopyrightNoticeAndPackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public enum " + javaName + " implements io.github.jwharm.javagi.base.Enumeration {\n");
        writer.increaseIndent();

        // Some enumerations contain duplicate members or members with invalid values
        // This filters them from inclusion as enum members.
        // Duplicates are added as public static final fields below
        List<Member> usable = memberList.stream()
                .filter(s -> s.usable)
                .collect(Collectors.groupingBy(s -> s.value, LinkedHashMap::new, Collectors.reducing((l, r) -> l)))
                .values().stream()
                .flatMap(Optional::stream)
                .toList();
        Member lastUsable = usable.get(usable.size() - 1);
        for (Member m : memberList) {
            if (usable.contains(m)) {
                writer.write("\n");
                if (m.doc != null) {
                    m.doc.generate(writer, false);
                }
                writer.write(m.name.toUpperCase() + "(" + m.value + ")");
                writer.write(m == lastUsable ? ";\n" : ",\n");
            } else if (! m.usable) {
                writer.write("// Skipped " + m.name.toUpperCase() + "(" + m.value + ")\n");
            }
        }

        // Add usable but duplicate members as public static final fields pointing to the member with the same value
        for (Member m : memberList) {
            if (m.usable && !usable.contains(m)) {
                Member u = null;
                for (Member u1 : usable) {
                    if (u1.value == m.value) u = u1;
                }
                if (u == null) System.out.println("Could not get corresponding enum member for " + m.name.toUpperCase());
                else {
                    writer.write("\n");
                    if (m.doc != null) {
                        m.doc.generate(writer, false);
                    }
                    writer.write("public static final " + javaName + " " + m.name.toUpperCase() + " = " + u.name.toUpperCase() + ";\n");
                }
            }
        }

        generateGType(writer);
        generateMemoryLayout(writer);

        writer.write("\n");
        writer.write("private final int value;\n");
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Create a new " + javaName + " for the provided value\n");
        writer.write(" * @param numeric value the enum value\n");
        writer.write(" */\n");
        writer.write(javaName + "(int value) {\n");
        writer.write("    this.value = value;\n");
        writer.write("}\n");
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Get the numeric value of this enum\n");
        writer.write(" * @return the enum value\n");
        writer.write(" */\n");
        writer.write("@Override\n");
        writer.write("public int getValue() {\n");
        writer.write("    return value;\n");
        writer.write("}\n");
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Create a new " + javaName + " for the provided value\n");
        writer.write(" * @param value the enum value\n");
        writer.write(" * @return the enum for the provided value\n");
        writer.write(" */\n");
        writer.write("public static " + javaName + " of(int value) {\n");
        writer.write("    return switch (value) {\n");
        for (Member m : usable) {
            writer.write("        case " + m.value + " -> " + m.name.toUpperCase() + ";\n");
        }
        writer.write("        default -> throw new IllegalStateException(\"Unexpected value: \" + value);\n");
        writer.write("    };\n");
        writer.write("}\n");
        
        generateMethodsAndSignals(writer);
        generateInjected(writer);

        writer.decreaseIndent();
        writer.write("}\n");
    }
    
    @Override
    public String getInteropString(String paramName, boolean isPointer) {
        String str = paramName + ".getValue()";
        if (isPointer) {
            return "new PointerInteger(" + str + ").handle()";
        } else {
            return str;
        }
    }
}
