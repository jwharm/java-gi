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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Bitfield extends ValueWrapper {

    public Bitfield(GirElement parent, String name, String cType, String getType, String version) {
        super(parent, name, null, cType, getType, version);
    }

    public void generate(SourceWriter writer) throws IOException {
        generateCopyrightNoticeAndPackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public class " + javaName + " extends io.github.jwharm.javagi.base.Bitfield {\n");
        writer.increaseIndent();

        generateGType(writer);

        // Filter duplicate members
        for (Member m : filterDuplicates(memberList)) {
            if (m.usable) {
                writer.write("\n");
                if (m.doc != null) {
                    m.doc.generate(writer, false);
                }
                writer.write("public static final " + javaName + " " + m.name.toUpperCase() + " = new " + javaName + "("+ m.value + ");\n");
            } else {
                writer.write("// Skipped: " + m.name.toUpperCase() + "\n");
            }
        }
        
        generateValueConstructor(writer, "int");
        generateMethodsAndSignals(writer);
        
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Combine (bitwise OR) operation\n");
        writer.write(" * @param masks one or more values to combine with\n");
        writer.write(" * @return the combined value by calculating {@code this | mask} \n");
        writer.write(" */\n");
        writer.write("public " + javaName + " or(" + javaName + "... masks) {\n");
        writer.write("    int value = this.getValue();\n");
        writer.write("    for (" + javaName + " arg : masks) {\n");
        writer.write("        value |= arg.getValue();\n");
        writer.write("    }\n");
        writer.write("    return new " + javaName + "(value);\n");
        writer.write("}\n");
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Combine (bitwise OR) operation\n");
        writer.write(" * @param mask the first value to combine\n");
        writer.write(" * @param masks the other values to combine\n");
        writer.write(" * @return the combined value by calculating {@code mask | masks[0] | masks[1] | ...} \n");
        writer.write(" */\n");
        writer.write("public static " + javaName + " combined(" + javaName + " mask, " + javaName + "... masks) {\n");
        writer.write("    int value = mask.getValue();\n");
        writer.write("    for (" + javaName + " arg : masks) {\n");
        writer.write("        value |= arg.getValue();\n");
        writer.write("    }\n");
        writer.write("    return new " + javaName + "(value);\n");
        writer.write("}\n");

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

    private List<Member> filterDuplicates(List<Member> input) {
        HashSet<String> set = new HashSet<>();
        ArrayList<Member> output = new ArrayList<>();

        for (Member m : input) {
            if (! set.contains(m.name)) {
                output.add(m);
                set.add(m.name);
            }
        }
        return output;
    }
}
