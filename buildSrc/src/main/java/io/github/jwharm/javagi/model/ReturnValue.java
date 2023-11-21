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

public class ReturnValue extends Parameter {

    public boolean returnsFloatingReference;
    public Type overrideReturnType;
    public String overrideReturnValue;

    public ReturnValue(GirElement parent, String transferOwnership, String nullable, String scope) {
        super(parent, null, transferOwnership, nullable,
                null, null, null, null, null, null, scope);

        returnsFloatingReference = false;
        overrideReturnType = null;
        overrideReturnValue = null;
    }

    @Override
    public boolean allocatesMemory() {
        return array != null
                || (type != null && type.isCallback() && scope.in(Scope.CALL, Scope.ASYNC));
    }

    /**
     * Generate code to process and return the function call result.
     * @param writer The source code file writer
     * @throws IOException Thrown when an error occurs while writing
     */
    public void generate(SourceWriter writer) throws IOException {
        // If the return value is an array, try to convert it to a Java array
        if (array != null) {
            String len = array.size(false);
            if (len != null) {
                if (nullable) {
                    writer.write("if (_result.equals(MemorySegment.NULL)) {\n");
                    writer.write("    return null;\n");
                    writer.write("}\n");
                }
                String valuelayout = Conversions.getValueLayoutPlain(array.type);
                if (array.type.isPrimitive && (!array.type.isBoolean())) {
                    // Array of primitive values
                    writer.write("return _result.reinterpret(" + len + " * " + valuelayout + ".byteSize(), _arena, null).toArray(" + valuelayout + ");\n");
                } else {
                    // Array of proxy objects
                    writer.write(array.type.qualifiedJavaType + "[] _resultArray = new " + array.type.qualifiedJavaType + "[" + len + "];\n");
                    writer.write("for (int _idx = 0; _idx < " + len + "; _idx++) {\n");
                    writer.write("    var _object = _result.get(" + valuelayout + ", _idx);\n");
                    writer.write("    _resultArray[_idx] = " + marshalNativeToJava(array.type, "_object", false) + ";\n");
                    writer.write("}\n");
                    writer.write("return _resultArray;\n");
                }
            } else {
                generateReturnStatement(writer);
            }
        } else {
            generateReturnStatement(writer);
        }
    }
    
    /**
     * Generate the return statement for a function or method call.
     * @param writer The source code file writer
     * @throws IOException Thrown when an error occurs while writing
     */
    public void generateReturnStatement(SourceWriter writer) throws IOException {
        Type type = overrideReturnType == null ? this.type : overrideReturnType;
        if (type != null && type.isVoid()) {
            return;
        }
        
        // When transfer-ownership="none", we must take a reference
        if (isGObject() && "none".equals(transferOwnership) && (! parent.name.equals("ref"))) {

            writer.write("var _object = ");
            marshalNativeToJava(writer, "_result", false);
            writer.write(";\n");
            writer.write("if (_object != null) {\n");

            // Debug logging
            writer.write("    GLibLogger.debug(\"Ref ");
            writeType(writer, false);
            writer.write(" %ld\\n\", _object == null || _object.handle() == null ? 0 : _object.handle());\n");

            writer.write("    _object.ref();\n");
            writer.write("}\n");
            writer.write("return _object;\n");

        } else if (type != null
                && (type.isUnion() || type.isRecord())
                && (! "org.gnome.gobject.TypeInstance".equals(type.qualifiedJavaType))
                && (! "org.gnome.gobject.TypeClass".equals(type.qualifiedJavaType))
                && (! "org.gnome.gobject.TypeInterface".equals(type.qualifiedJavaType))) {

            writer.write("var _instance = ");
            marshalNativeToJava(writer, "_result", false);
            writer.write(";\n");
            writer.write("if (_instance != null) {\n");
            writer.increaseIndent();
            writer.write("MemoryCleaner.takeOwnership(_instance.handle());\n");

            RegisteredType rt = type.girElementInstance;
            if (rt != null) {
                String classname = type.qualifiedJavaType;
                rt.generateSetFreeFunc(writer, "_instance", classname);
            }

            writer.decreaseIndent();
            writer.write("}\n");
            writer.write("return _instance;\n");

        } else {

            writer.write("return ");
            marshalNativeToJava(writer, "_result", false);
            writer.write(";\n");
        }
    }

    @Override
    public Type getType() {
        return overrideReturnType != null ? overrideReturnType : super.getType();
    }

    public boolean isVoid() {
        if (overrideReturnType != null)
            return (overrideReturnType.isVoid());
        return (type != null && type.isVoid());
    }

    public String getCarrierType() {
        if (overrideReturnType != null)
            return Conversions.getCarrierType(overrideReturnType);
        return Conversions.getCarrierType(type);
    }
}
