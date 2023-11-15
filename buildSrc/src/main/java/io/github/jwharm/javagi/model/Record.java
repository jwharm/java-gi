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

public class Record extends Class {

    public final String disguised;
    public final String isGTypeStructFor;
    public final String foreign;

    public Record(GirElement parent, String name, String cType, String getType, String version, String disguised, String isGTypeStructFor, String foreign) {
        super(parent, name, null, cType, null, getType, null, null, null, version, null, null);
        this.disguised = disguised;
        this.isGTypeStructFor = isGTypeStructFor;
        this.foreign = foreign;
    }
    
    /**
     * A record in GI is a struct in C. Java doesn't have a struct type, and the java 'record'
     * functionality has a different purpose. So the generated API creates a class that
     * extends StructProxy (instead of GObject).
     * Structs are often initialized implicitly, which means they don't always have constructors.
     * To solve this, we generate a static allocate() function that allocates a memory segment.
     */
    public void generate(SourceWriter writer) throws IOException {
        if (isGTypeStructFor != null) {
            writer.write("\n");
        } else {
            generateCopyrightNoticeAndPackageDeclaration(writer);
            generateImportStatements(writer);
        }
        
        generateJavadoc(writer);
        writer.write("public ");
        
        if (isGTypeStructFor != null) {
            writer.write("static ");
        }
        
        writer.write("class " + javaName);
        
        if (generic) {
            writer.write("<T extends org.gnome.gobject.GObject>");
        }

        if (isGTypeStructFor != null) {
            // parent_class is always the first field, unless the struct is disguised
            if (fieldList.isEmpty()) {
                RegisteredType outerClass = module().cTypeLookupTable.get(isGTypeStructFor);
                writer.write(" extends org.gnome.gobject."
                        + (outerClass instanceof Interface ? "TypeInterface" : "TypeClass"));
            } else {
                String parentCType = fieldList.get(0).type.cType;
                Record parentRec = (Record) module().cTypeLookupTable.get(parentCType);
                if (parentRec.isGTypeStructFor != null) {
                    String parentClass = Conversions.toQualifiedJavaType(parentRec.isGTypeStructFor, parentRec.getNamespace());
                    String parentStr = parentClass + "." + parentRec.javaName;
                    writer.write(" extends " + parentStr);
                } else {
                    String parentClass = Conversions.toQualifiedJavaType(parentRec.name, parentRec.getNamespace());
                    writer.write(" extends " + parentClass);
                }
            }
        } else if ("GTypeInstance".equals(cType) || "GTypeClass".equals(cType) || "GTypeInterface".equals(cType)) {
            writer.write(" extends ProxyInstance");
        } else {
            writer.write(" extends ManagedInstance");
        }

        // Floating
        if (isFloating()) {
            writer.write(" implements io.github.jwharm.javagi.base.Floating");
        }

        writer.write(" {\n");
        writer.increaseIndent();

        if (isGTypeStructFor == null) {
            generateEnsureInitialized(writer);
        }
        generateGType(writer);

        // Opaque structs have unknown memory layout and should not have an allocator
        if (! (isOpaqueStruct() || hasOpaqueStructFields())) {
            generateMemoryLayout(writer);
            generateRecordAllocator(writer);
            for (Field f : fieldList) {
                f.generate(writer);
            }
            // Fields can be inside a <union> tag
            if (! unionList.isEmpty()) {
                for (Field f : unionList.get(0).fieldList) {
                    f.generate(writer);
                }
            }
        }

        generateMemoryAddressConstructor(writer);
        generateConstructors(writer);
        generateMethodsAndSignals(writer);
        generateInjected(writer);

        // Generate a custom gtype declaration for GVariant
        if (isInstanceOf("org.gnome.glib.Variant") && "intern".equals(getType)) {
            writer.write("\n");
            writer.write("/**\n");
            writer.write(" * Get the GType of the " + cType + " class.\n");
            writer.write(" * @return the GType");
            writer.write(" */\n");
            writer.write("public static org.gnome.glib.Type getType() {\n");
            // Types.VARIANT is declared in GObject. Hard-coded value as workaround
            writer.write("    return new org.gnome.glib.Type(21L << 2);\n");
            writer.write("}\n");
        }

        writer.decreaseIndent();
        writer.write("}\n");
    }

    public void generateRecordAllocator(SourceWriter writer) throws IOException {
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Allocate a new {@link " + javaName + "}.\n");
        writer.write(" * \n");
        writer.write(" * @param  arena to control the memory allocation scope.\n");
        writer.write(" * @return A new, uninitialized {@link " + javaName + "}\n");
        writer.write(" */\n");
        writer.write("public static " + javaName + " allocate(Arena arena) {\n");
        writer.write("    MemorySegment segment = arena.allocate(getMemoryLayout());\n");
        writer.write("    return new " + javaName + "(segment);\n");
        writer.write("}\n");

        // Generate deprecated method for backward compatibility
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Allocate a new {@link " + javaName + "}. A {@link java.lang.ref.Cleaner} \n");
        writer.write(" * is assigned to the allocated memory segment that will release the \n");
        writer.write(" * memory when the {@link " + javaName + "} instance is garbage-collected.\n");
        writer.write(" * \n");
        writer.write(" * @return A new, uninitialized {@link " + javaName + "}\n");
        writer.write(" * @deprecated See {@link #allocate(Arena)}\n");
        writer.write(" */\n");
        writer.write("@Deprecated\n");
        writer.write("public static " + javaName + " allocate() {\n");
        writer.write("    return " + javaName + ".allocate(Arena.ofAuto());\n");
        writer.write("}\n");

        // For regular structs (not typeclasses), generate a second allocator function
        // that takes values for all fields, so it becomes possible to quickly allocate
        // a struct. For example: var color = RGBA.allocate(0.6F, 0.5F, 0.9F, 1.0F);
        if (isGTypeStructFor == null) {

            // First, determine if this struct has field setters
            boolean hasSetters = false;
            for (Field field : fieldList) {
                if (field.disguised()) {
                    continue;
                }
                if (field.callback != null) {
                    continue;
                }
                hasSetters = true;
                break;
            }
            if (! hasSetters) {
                return;
            }

            // Generate deprecated method for backward compatibility
            writer.write("\n");
            writer.write("/**\n");
            writer.write(" * Allocate a new {@link " + javaName + "} with the fields set to the provided values. \n");
            writer.write(" * A {@link java.lang.ref.Cleaner} is assigned to the allocated memory segment that will \n");
            writer.write(" * release the memory when the {@link " + javaName + "} instance is garbage-collected.\n");
            writer.write(" * \n");
            for (Field field : fieldList) {
                // Ignore disguised fields
                if (field.disguised()) {
                    continue;
                }
                writer.write(" * @param ");
                field.writeName(writer);
                writer.write((field.callback == null ? " value " : " callback function ") + "for the field {@code ");
                field.writeName(writer);
                writer.write("}\n");
            }
            writer.write(" * @return A new {@link " + javaName + "} with the fields set to the provided values\n");
            writer.write(" * @deprecated See {@link #allocate(Arena");
            for (Field field : fieldList) {
                if (field.disguised()) {
                    continue;
                }
                writer.write(", ");
                field.writeType(writer, false);
            }
            writer.write(")}\n");
            writer.write(" */\n");
            writer.write("@Deprecated\n");
            writer.write("public static " + javaName + " allocate(");
            boolean first = true;
            for (Field field : fieldList) {
                if (field.disguised()) {
                    continue;
                }
                if (first)
                    first = false;
                else
                    writer.write(", ");
                field.writeTypeAndName(writer);
            }
            writer.write(") {\n");
            writer.increaseIndent();
            writer.write("return allocate(Arena.ofAuto()");
            for (Field field : fieldList) {
                if (field.disguised()) {
                    continue;
                }
                writer.write(", ");
                field.writeName(writer);
            }
            writer.write(");\n");
            writer.decreaseIndent();
            writer.write("}\n");
            // End of temporary (deprecated) method

            writer.write("\n");
            writer.write("/**\n");
            writer.write(" * Allocate a new {@link " + javaName + "} with the fields set to the provided values. \n");
            writer.write(" * \n");
            writer.write(" * @param arena to control the memory allocation scope.\n");
            // Write javadoc for parameters
            for (Field field : fieldList) {
                // Ignore disguised fields
                if (field.disguised()) {
                    continue;
                }
                writer.write(" * @param ");
                field.writeName(writer);
                writer.write((field.callback == null ? " value " : " callback function ") + "for the field {@code ");
                field.writeName(writer);
                writer.write("}\n");
            }
            writer.write(" * @return A new {@link " + javaName + "} with the fields set to the provided values\n");
            writer.write(" */\n");
            writer.write("public static " + javaName + " allocate(Arena arena");

            // Write parameters
            for (Field field : fieldList) {
                // Ignore disguised fields
                if (field.disguised()) {
                    continue;
                }
                writer.write(", ");
                field.writeTypeAndName(writer);
            }
            writer.write(") {\n");
            writer.increaseIndent();

            // Call the allocate() method
            writer.write(javaName + " _instance = allocate(arena);\n");

            // Call the field setters
            for (Field field : fieldList) {
                // Ignore disguised fields
                if (field.disguised()) {
                    continue;
                }
                writer.write("_instance." + (field.callback == null ? "write" : "override"));
                writer.write(Conversions.toCamelCase(field.name, true) + "(");
                if (field.allocatesMemory()) {
                    writer.write("arena, ");
                }
                field.writeName(writer);
                writer.write(");\n");
            }

            // Return the new instance
            writer.write("return _instance;\n");
            writer.decreaseIndent();
            writer.write("}\n");
        }
    }
}
