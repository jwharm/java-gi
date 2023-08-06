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

import java.io.IOException;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

public class Constant extends GirElement {

    public final String value, cType;

    public Constant(GirElement parent, String name, String value, String cType) {
        super(parent);
        this.name = name;
        this.value = value;
        this.cType = cType;
    }

    public void generate(SourceWriter writer) throws IOException {
        String typeStr = "unknown type";
        String printValue;
        try {
            if (type.isAliasForPrimitive()) {
                typeStr = type.girElementInstance.type.simpleJavaType;
                printValue = "new " + type.qualifiedJavaType + "(" + Conversions.literal(typeStr, value) + ")";
            } else if (type.isBitfield() || type.isEnum()) {
                typeStr = "int";
                printValue = "new " + type.qualifiedJavaType + "(" + Conversions.literal(typeStr, value) + ")";
            } else {
                typeStr = type.qualifiedJavaType;
                printValue = Conversions.literal(typeStr, value);
            }
        } catch (NumberFormatException nfe) {
            // Do not write anything
            System.out.println("Skipping <constant name=\"" + name + "\""
                    + " value=\"" + value + "\""
                    + ">: Value not allowed for " + typeStr);
            return;
        }
        
        writer.write("    \n");
        
        // Documentation
        if (doc != null) {
            doc.generate(writer, false);
        }
        writer.write("public static final " + type.qualifiedJavaType + " " + name + " = " + printValue + ";\n");
    }
}
