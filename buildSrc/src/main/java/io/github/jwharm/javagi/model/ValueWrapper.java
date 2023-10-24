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

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;

public abstract class ValueWrapper extends RegisteredType {
    
    public ValueWrapper(GirElement parent, String name, String parentClass, String cType, String getType, String version) {
        super(parent, name, parentClass, cType, getType, version);
    }
    
    public void generateValueConstructor(SourceWriter writer, String typeStr) throws IOException {
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Create a new " + javaName + " with the provided value\n");
        writer.write(" */\n");
        writer.write("public " + javaName + "(" + typeStr + " value) {\n");
        writer.write("    super(value);\n");
        writer.write("}\n");
    }
    
    @Override
    public String getInteropString(String paramName, boolean isPointer, Scope scope) {
        String str = paramName + ".getValue()." + type.qualifiedJavaType + "Value()";
        if ("java.lang.foreign.MemorySegment".equals(type.qualifiedJavaType)) {
            str = paramName + ".getValue()";
        }
        if (isPointer) {
            return "new Pointer" + Conversions.primitiveClassName(type.qualifiedJavaType) + "(" + str + ").handle()";
        } else {
            return str;
        }
    }
}
