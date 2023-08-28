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
import io.github.jwharm.javagi.generator.Builder;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;
import java.util.Objects;
import java.util.StringJoiner;

public class Class extends RegisteredType {
    
    public String typeName;
    public String typeStruct;
    public String getValueFunc;
    public String setValueFunc;
    public String abstract_;
    public String final_;
    
    public Record classStruct;

    public Class(GirElement parent, String name, String parentClass, String cType, String typeName, String getType,
            String typeStruct, String getValueFunc, String setValueFunc, String version, String abstract_, String final_) {
        
        super(parent, name, parentClass, cType, getType, version);
        this.typeName = typeName;
        this.typeStruct = typeStruct;
        this.getValueFunc = getValueFunc;
        this.setValueFunc = setValueFunc;
        this.abstract_ = abstract_;
        this.final_ = final_;
    }

    public void generate(SourceWriter writer) throws IOException {
        classStruct = (Record) module().cTypeLookupTable.get(getNamespace().cIdentifierPrefix + typeStruct);
        
        generateCopyrightNoticeAndPackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public ");

        // Abstract classes
        if ("1".equals(abstract_)) {
            writer.write("abstract ");
        }

        // Final classes
        if ("1".equals(final_)) {
            writer.write("final ");
        }

        writer.write("class " + javaName);

        // Generic types
        if (generic) {
            writer.write("<T extends org.gnome.gobject.GObject>");
        }

        // Parent class
        writer.write(" extends ");
        writer.write(Objects.requireNonNullElse(parentClass, "org.gnome.gobject.TypeInstance"));

        // Interfaces
        StringJoiner interfaces = new StringJoiner(", ", " implements ", "").setEmptyValue("");
        implementsList.forEach(impl -> interfaces.add(impl.getQualifiedJavaName()));
        if (autoCloseable) {
            interfaces.add("io.github.jwharm.javagi.gio.AutoCloseable");
        }
        if (isFloating()) {
            interfaces.add("io.github.jwharm.javagi.base.Floating");
        }
        writer.write(interfaces + " {\n");
        writer.increaseIndent();

        generateMemoryAddressConstructor(writer);
        generateEnsureInitialized(writer);
        generateGType(writer);
        generateMemoryLayout(writer);
        generateParentAccessor(writer);
        generateConstructors(writer);
        generateMethodsAndSignals(writer);

        if (classStruct != null) {
            classStruct.generate(writer);
        }
        
        if (isInstanceOf("org.gnome.gobject.GObject")) {
            Builder.generateBuilder(writer, this);
        }

        // Generate a custom gtype declaration for ParamSpec
        if (isInstanceOf("org.gnome.gobject.ParamSpec") && "intern".equals(getType)) {
            writer.write("\n");
            writer.write("/**\n");
            writer.write(" * Get the GType of the " + cType + " class.\n");
            writer.write(" * @return the GType");
            writer.write(" */\n");
            writer.write("public static org.gnome.glib.Type getType() {\n");
            writer.write("    return Types.PARAM;\n");
            writer.write("}\n");
        }

        // Abstract classes
        if ("1".equals(abstract_)) {
            generateImplClass(writer);
        }

        generateInjected(writer);

        writer.decreaseIndent();
        writer.write("}\n");
    }

    @Override
    public String getConstructorString() {
        String qName = Conversions.convertToJavaType(this.javaName, true, getNamespace());
        return ("1".equals(abstract_)
                ? qName + "." + this.javaName + "Impl::new"
                : qName + "::new");
    }

    /**
     * Check if this method must do a platorm compatibility check and throw UnsupportedPlatformException.
     * @return whether to do a cross-platform availability check.
     */
    @Override
    public boolean doPlatformCheck() {
        return platforms.size() < 3;
    }
}
