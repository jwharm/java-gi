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

public class Interface extends RegisteredType {

    public String typeName;
    public String typeStruct;
    public Prerequisite prerequisite;

    public Record classStruct;
    
    public Interface(GirElement parent, String name, String cType, String typeName, String getType,
            String typeStruct, String version) {
        
        super(parent, name, null, cType, getType, version);
        this.typeName = typeName;
        this.typeStruct = typeStruct;
    }

    public void generate(SourceWriter writer) throws IOException {
        classStruct = (Record) module().cTypeLookupTable.get(getNamespace().cIdentifierPrefix + typeStruct);
        
        generateCopyrightNoticeAndPackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public interface " + javaName);
        if (generic) {
            writer.write("<T extends org.gnome.gobject.GObject>");
        }
        writer.write(" extends io.github.jwharm.javagi.base.Proxy {\n");
        writer.increaseIndent();

        generateGType(writer);
        generateMethodsAndSignals(writer);

        if (classStruct != null) {
            classStruct.generate(writer);
        }
        
        Builder.generateInterfaceBuilder(writer, this);
        generateImplClass(writer);
        generateInjected(writer);

        writer.decreaseIndent();
        writer.write("}\n");
    }

    @Override
    public String getConstructorString() {
        String qName = Conversions.convertToJavaType(this.javaName, true, getNamespace());
        return qName + "." + this.javaName + "Impl::new";
    }
}
