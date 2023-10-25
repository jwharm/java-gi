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

public class Property extends Variable {

    public final String propertyName, transferOwnership, getter;

    public Property(GirElement parent, String name, String transferOwnership, String getter) {
        super(parent);
        this.propertyName = name;
        this.name = Conversions.toLowerCaseJavaName(name);
        this.transferOwnership = transferOwnership;
        this.getter = getter;
    }
    
    /**
     * Generate a setter method for use in a GObjectBuilder
     * @param writer The writer to the class file
     * @throws IOException Thrown when an exception occurs during writing
     */
    public void generate(SourceWriter writer) throws IOException {
        String gTypeDeclaration = getGTypeDeclaration();
        writer.write("\n");
        if (doc != null) {
            doc.generate(writer, false);
        }
        writer.write((parent instanceof Interface) ? "default " : "public ");
        writer.write("S " + Conversions.toLowerCaseJavaName(name) + "(");
        writeTypeAndName(writer);
        writer.write(") {\n");
        writer.increaseIndent();
        if (allocatesMemory()) {
            writer.write("Arena _arena = getArena();\n");
            writer.write("org.gnome.gobject.Value _value = org.gnome.gobject.Value.allocate(_arena);\n");
        } else {
            writer.write("org.gnome.gobject.Value _value = org.gnome.gobject.Value.allocate(getArena());\n");
        }
        writer.write("_value.init(" + gTypeDeclaration + ");\n");
        writer.write(getValueSetter("_value", gTypeDeclaration, name) + ";\n");
        writer.write("addBuilderProperty(\"" + propertyName + "\", _value);\n");
        writer.write("return (S) this;\n");
        writer.decreaseIndent();
        writer.write("}\n");
    }
}
