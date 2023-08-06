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
import java.util.stream.Stream;

public class Signal extends Method implements Closure {

    public final String when;
    private final String signalName;
    private final String qualifiedName;
    public boolean detailed;

    public Signal(GirElement parent, String name, String when, String detailed, String deprecated, String throws_) {
        super(parent, name, null, deprecated, throws_, null, null, null);
        this.when = when;
        this.detailed = "1".equals(detailed);

        String className = ((RegisteredType) parent).javaName;
        signalName = Conversions.toSimpleJavaType(name, getNamespace());
        qualifiedName = className + "." + signalName;
    }

    public void generate(SourceWriter writer) throws IOException {
        writer.write("\n");
        generateFunctionalInterface(writer, signalName);
        writer.write("\n");

        // Generate signal connect function
        if (doc != null) {
            doc.generate(writer, true);
        }

        // Deprecation
        if ("1".equals(deprecated)) {
            writer.write("@Deprecated\n");
        }

        writer.write("public " + (parent instanceof Interface ? "default " : "") + "SignalConnection<" + qualifiedName + "> on" + signalName + "(");
        
        // For detailed signals like GObject.notify::..., generate a String parameter to specify the detailed signal
        if (detailed) {
            writer.write("@Nullable String detail, ");
        }
        
        writer.write(qualifiedName + " handler) {\n");
        writer.increaseIndent();
        
        writer.write("try (Arena _arena = Arena.openConfined()) {\n");
        writer.increaseIndent();
        writer.write("try {\n");
        writer.write("    var _result = (long) Interop.g_signal_connect_data.invokeExact(\n");
        writer.write("        handle(), Interop.allocateNativeString(\"" + name + "\"");
        if (detailed) {
            writer.write(" + ((detail == null || detail.isBlank()) ? \"\" : (\"::\" + detail))");
        }
        writer.write(", _arena), handler.toCallback(), MemorySegment.NULL, MemorySegment.NULL, 0);\n");
        writer.write("    return new SignalConnection<>(handle(), _result);\n");
        writer.write("} catch (Throwable _err) {\n");
        writer.write("    throw new AssertionError(\"Unexpected exception occured: \", _err);\n");
        writer.write("}\n");
        writer.decreaseIndent();
        writer.write("}\n");

        writer.decreaseIndent();
        writer.write("}\n");

        // Check if an emit function already is defined in the GIR file
        boolean emitFunctionExists = false;
        for (Method method : Stream.concat(parent.methodList.stream(), parent.functionList.stream()).toList()) {
            String n = Conversions.toLowerCaseJavaName(method.name);
            if (n.equals("emit" + signalName)) emitFunctionExists = true;
        }

        // Generate signal emit function
        if (!emitFunctionExists) {
            writer.write("\n");
            writer.write("/**\n");
            writer.write(" * Emits the \"" + name + "\" signal. See {@link #on" + signalName + "}.\n");
            writer.write(" */\n");

            // Deprecation
            if ("1".equals(deprecated)) {
                writer.write("@Deprecated\n");
            }

            writer.write("public " + (parent instanceof Interface ? "default " : ""));
            returnValue.writeType(writer, true, true);
            writer.write(" emit" + signalName + "(");
            if (detailed) {
                writer.write("@Nullable String detail");
            }

            if (parameters != null) {
                if (detailed) {
                    writer.write(", ");
                }
                parameters.generateJavaParameters(writer, false);
            }

            writer.write(") {\n");
            writer.increaseIndent();
            writer.write("try (Arena _arena = Arena.openConfined()) {\n");
            writer.increaseIndent();
            if (parameters != null) {
                parameters.generatePreprocessing(writer);
            }
            boolean hasReturn = returnValue.type != null && !"void".equals(returnValue.type.simpleJavaType);
            if (hasReturn) {
                writer.write("MemorySegment _result = _arena.allocate(" + Conversions.getValueLayout(returnValue.type) + ");\n");
            }
            writer.write("Interop.g_signal_emit_by_name.invokeExact(\n");
            writer.write("        handle(),\n");
            writer.write("        Interop.allocateNativeString(\"" + name + "\"");
            if (detailed) {
                writer.write(" + ((detail == null || detail.isBlank()) ? \"\" : (\"::\" + detail))");
            }
            writer.write(", _arena)");
            if (parameters != null || hasReturn) {
                writer.increaseIndent();
                writer.write(",\n");
                writer.write("    new Object[] {");
                if (parameters != null) {
                    if (parameters.parameterList.size() == 1) {
                        writer.write("\n");
                        writer.write("        ");
                    }
                    parameters.marshalJavaToNative(writer, null);
                }
                if (hasReturn) {
                    writer.write(parameters == null ? "\n" : ",\n");
                    writer.write("        _result");
                }
                writer.write("\n");
                writer.write("    }\n");
                writer.decreaseIndent();
            } else {
                writer.write(", new Object[0]");
            }
            writer.write(");\n");
            if (parameters != null) {
                parameters.generatePostprocessing(writer);
            }
            if (hasReturn) {
                writer.write("return ");
                returnValue.marshalNativeToJava(writer, "_result.get(" + Conversions.getValueLayout(returnValue.type) + ", 0)", false);
                writer.write(";\n");
            }
            writer.decreaseIndent();
            writer.write("} catch (Throwable _err) {\n");
            writer.write("    throw new AssertionError(\"Unexpected exception occured: \", _err);\n");
            writer.write("}\n");
            writer.decreaseIndent();
            writer.write("}\n");
        }
    }
}
