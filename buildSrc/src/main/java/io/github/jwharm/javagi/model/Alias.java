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

/**
 * Represents an {@code alias} element
 */
public class Alias extends ValueWrapper {

    /**
     * Represents the different types of elements which may be aliased
     */
    public enum TargetType {
        ALIAS, CLASS, RECORD, INTERFACE, CALLBACK, VALUE, UNKNOWN
    }

    /**
     * Gets the type of element that this alias targets
     */
    public TargetType getTargetType() {
        if (type.isPrimitive
                || "java.lang.String".equals(type.qualifiedJavaType)
                || "java.lang.foreign.MemorySegment".equals(type.qualifiedJavaType)) {
            return TargetType.VALUE;
        } else if (type.girElementInstance instanceof Alias) {
            return TargetType.ALIAS;
        } else if (type.girElementInstance instanceof Callback) {
            return TargetType.CALLBACK;
        } else if (type.girElementInstance instanceof Interface) {
            return TargetType.INTERFACE;
        } else if (type.girElementInstance instanceof Record) {
            return TargetType.RECORD;
        } else if (type.girElementInstance instanceof Class) {
            return TargetType.CLASS;
        }
        return TargetType.UNKNOWN;
    }

    public Alias(GirElement parent, String name, String cType, String getType, String version) {
        super(parent, name, null, cType, getType, version);
    }

    // Aliases (typedefs) don't exist in Java. We can emulate this using inheritance.
    // For primitives and Strings, we wrap the value.
    public void generate(SourceWriter writer) throws IOException {
        generateCopyrightNoticeAndPackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        switch (getTargetType()) {
            case ALIAS -> {
                writer.write("public class " + javaName + " extends " + type.qualifiedJavaType + " {\n");
                writer.increaseIndent();
                generateValueConstructor(writer, type.girElementInstance.type.qualifiedJavaType);
            }
            case CLASS, RECORD -> {
                writer.write("public class " + javaName);
                if (generic)
                    writer.write("<T extends org.gnome.gobject.GObject>");
                if (type.qualifiedJavaType.equals("void")) {
                    writer.write(" extends org.gnome.gobject.GObject {\n");
                } else {
                    writer.write(" extends " + type.qualifiedJavaType + " {\n");
                }
                writer.increaseIndent();
                generateMemoryAddressConstructor(writer);
            }
            case INTERFACE, CALLBACK -> {
                writer.write("public interface " + javaName);
                if (generic)
                    writer.write("<T extends org.gnome.gobject.GObject>");
                writer.write(" extends " + type.qualifiedJavaType + " {\n");
                writer.increaseIndent();
            }
            case VALUE -> {
                String genericType = Conversions.primitiveClassName(type.qualifiedJavaType);
                if ("utf8".equals(type.name)) {
                    genericType = "java.lang.String";
                } else if ("java.lang.foreign.MemorySegment".equals(type.qualifiedJavaType)) {
                    genericType = type.qualifiedJavaType;
                }
                writer.write("public class " + javaName + " extends io.github.jwharm.javagi.base.Alias<" + genericType + "> {\n");
                writer.increaseIndent();
                generateValueConstructor(writer, type.qualifiedJavaType);
                generateArrayConstructor(writer);
            }
            default -> {
                writer.write("public class " + javaName + " {\n");
                writer.increaseIndent();
            }
        }
        generateGType(writer);
        generateInjected(writer);
        writer.decreaseIndent();
        writer.write("}\n");
    }

    protected void generateArrayConstructor(SourceWriter writer) throws IOException {
        String layout = Conversions.getValueLayoutPlain(type);
        writer.write("\n");
        writer.write("@ApiStatus.Internal\n");
        writer.write("public static " + javaName + "[] fromNativeArray(MemorySegment address, long length, boolean free) {\n");
        
        writer.write("    " + javaName + "[] array = new " + javaName + "[(int) length];\n");
        writer.write("    long bytesSize = " + layout + ".byteSize();\n");
        writer.write("    MemorySegment segment = address.reinterpret(bytesSize * length);\n");
        writer.write("    for (int i = 0; i < length; i++) {\n");
        
        if ("utf8".equals(type.name)) {
            writer.write("        array[i] = new " + javaName + "(Interop.getStringFrom(segment.get(" + layout + ", i * bytesSize), free));\n");
        } else {
            writer.write("        array[i] = new " + javaName + "(segment.get(" + layout + ", i * bytesSize));\n");
        }
        
        writer.write("    }\n");
        writer.write("    if (free) {\n");
        writer.write("        org.gnome.glib.GLib.free(address);\n");
        writer.write("    }\n");
        writer.write("    return array;\n");
        writer.write("}\n");
    }

    @Override
    public String getInteropString(String paramName, boolean isPointer, Scope scope) {
        if (getTargetType() == TargetType.VALUE) {
            return super.getInteropString(paramName, isPointer, scope);
        } else {
            return type.girElementInstance.getInteropString(paramName, isPointer, scope);
        }
    }
}
