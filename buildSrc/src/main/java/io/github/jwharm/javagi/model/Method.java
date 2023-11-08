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
import java.io.StringWriter;

import io.github.jwharm.javagi.configuration.Settings;
import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

public class Method extends GirElement implements CallableType {

    public final String cIdentifier;
    public final String throws_;
    public final String shadowedBy;
    public final String shadows;
    public final String movedTo;

    public ReturnValue returnValue;
    public Parameters parameters;

    public String visibility;
    public boolean skip = false;

    public VirtualMethod linkedVirtualMethod = null;

    private String signature = null;

    public Method(GirElement parent, String name, String cIdentifier, String deprecated,
                  String throws_, String shadowedBy, String shadows, String movedTo) {
        super(parent);
        // Language bindings are expected to rename a function to the shadowed function
        this.name = shadows == null ? name : shadows;
        this.cIdentifier = cIdentifier;
        this.deprecated = deprecated;
        this.throws_ = throws_;
        this.shadowedBy = shadowedBy;
        this.shadows = shadows;
        this.movedTo = movedTo;
        this.visibility = (parent instanceof Interface) ? "default" : "public";

        // Generated under another name
        if (shadowedBy != null) {
            this.skip = true;
        }

        // Rename methods with "moved-to" attribute, but skip generation if the "moved-to" name has
        // the form of "Type.new_name", because in that case it already exists under the new name.
        if (movedTo != null) {
            if (movedTo.contains(".")) {
                this.skip = true;
            } else {
                this.name = movedTo;
            }
        }
    }

    public String getMethodDeclaration() {
        SourceWriter writer = new SourceWriter(new StringWriter());
        
        try {
            // Visibility
            if (! visibility.isEmpty()) {
                writer.write(visibility + " ");
            }

            // Static methods (functions and constructor helpers)
            if (this instanceof Function || this instanceof Constructor) {
                writer.write("static ");
            }

            // Return type
            if (this instanceof Constructor) {
                writer.write("MemorySegment");
            } else {
                getReturnValue().writeType(writer, true);
            }

            // Method name
            String methodName = Conversions.toLowerCaseJavaName(name);

            // Constructor helper
            if (this instanceof Constructor) {
                methodName = "construct" + Conversions.toCamelCase(name, true);
            }

            // Overriding toString() in a default method is not allowed.
            if (parent instanceof Interface) {
                methodName = Conversions.replaceJavaObjectMethodNames(methodName);
            }
            writer.write(" " + methodName + "(");

            // Parameters
            if (getParameters() != null) {
                getParameters().generateJavaParameters(writer);
            }
            writer.write(")");

            // Exceptions
            if (throws_ != null) {
                writer.write(" throws GErrorException");
            }
        } catch (IOException ignored) {
            // StringWriter will never throw IOException
        }
        
        return writer.toString();
    }

    /**
     * Generates a String that describes the type signature of this method.
     * The result is cached for performance reasons.
     */
    public String getNameAndSignature() {
        if (this.signature != null) {
            return this.signature;
        }

        SourceWriter writer = new SourceWriter(new StringWriter());

        try {
            // Method name
            String methodName = Conversions.toLowerCaseJavaName(name);
            if (parent instanceof Interface) { // Overriding toString() in a default method is not allowed.
                methodName = Conversions.replaceJavaObjectMethodNames(methodName);
            }
            writer.write(" " + methodName + "(");

            // Parameters
            if (getParameters() != null) {
                getParameters().generateJavaParameterTypes(writer, false);
            }
            writer.write(")");

            // Exceptions
            if (throws_ != null) {
                writer.write(" throws GErrorException");
            }
        } catch (IOException ignored) {
            // StringWriter will never throw IOException
        }

        // Cache the result for future invocations
        this.signature = writer.toString();
        return this.signature;
    }

    public void generate(SourceWriter writer) throws IOException {
        if (skip) {
            return;
        }

        writer.write("\n");
        
        // Documentation
        if (this instanceof Constructor) {
            writer.write("/**\n");
            writer.write(" * Helper function for the {@code " + name + "} constructor\n");
            writer.write(" */\n");
        } else if (doc != null) {
            doc.generate(writer, false);
        }

        // Deprecation
        if ("1".equals(deprecated) || hasVaListParameter()) {
            writer.write("@Deprecated\n");
        }

        // Declaration
        writer.write(getMethodDeclaration());
        
        writer.write(" {\n");
        writer.increaseIndent();

        // Check for unsupported platforms
        generatePlatformCheck(writer);

        // FunctionDescriptor of the native function signature
        writer.write("FunctionDescriptor _fdesc = ");
        boolean varargs = generateFunctionDescriptor(writer);
        writer.write(";\n");

        // Generate try-with-resources?
        boolean hasScope = allocatesMemory();
        if (hasScope) {
            writer.write("try (Arena _arena = Arena.ofConfined()) {\n");
            writer.increaseIndent();
        }

        // Generate preprocessing statements for all parameters
        if (parameters != null) {
            parameters.generatePreprocessing(writer);
        }

        // Allocate GError pointer
        if (throws_ != null) {
            writer.write("MemorySegment _gerror = _arena.allocate(ValueLayout.ADDRESS);\n");
        }
        
        // Variable declaration for return value
        String carrierType = Conversions.getCarrierType(getReturnValue().type);
        if (! (returnValue.type != null && returnValue.type.isVoid())) {
            writer.write(carrierType + " _result;\n");
        }

        // The method call is wrapped in a try-catch block
        writer.write("try {\n");
        writer.increaseIndent();

        // If this method is also a virtual method, and callParent() is set, call the parent virtual method
        if (linkedVirtualMethod != null) {
            writer.write("if (");
            if (parent instanceof Interface) {
                writer.write("((org.gnome.gobject.TypeInstance) this).");
            }
            writer.write("callParent()) {\n");
            writer.increaseIndent();
            linkedVirtualMethod.generateInvocation(writer);
            writer.decreaseIndent();
            writer.write("} else {\n");
            writer.increaseIndent();
        }

        // Log the method call
        log(cIdentifier, writer);

        // Generate the return type
        if (! (returnValue.type != null && returnValue.type.isVoid())) {
            writer.write("_result = (");
            writer.write(carrierType);
            writer.write(") ");
        }

        // Invoke to the method handle
        writer.write("Interop.downcallHandle(\"" + cIdentifier + "\", _fdesc, " + varargs + ").invokeExact(");

        // Marshall the parameters to the native types
        if (parameters != null) {
            parameters.marshalJavaToNative(writer, throws_);
        }
        writer.write(");\n");

        // End of if/else block for virtual/named method call
        if (linkedVirtualMethod != null) {
            writer.decreaseIndent();
            writer.write("}\n");
        }

        // If something goes wrong in the invokeExact() call
        writer.decreaseIndent();
        writer.write("} catch (Throwable _err) {\n");
        writer.write("    throw new AssertionError(\"Unexpected exception occurred: \", _err);\n");
        writer.write("}\n");

        // Throw GErrorException
        if (throws_ != null) {
            writer.write("if (GErrorException.isErrorSet(_gerror)) {\n");
            writer.write("    throw new GErrorException(_gerror);\n");
            writer.write("}\n");
        }
        
        // Generate post-processing actions for parameters
        if (parameters != null) {
            parameters.generatePostprocessing(writer);
        }

        // Generate code to process and return the result value
        if (this instanceof Constructor) {
            writer.write("return _result;\n");
        } else {
            returnValue.generate(writer);
        }

        // End of memory allocation scope
        if (hasScope) {
            writer.decreaseIndent();
            writer.write("}\n");
        }

        writer.decreaseIndent();
        writer.write("}\n");
    }

    /**
     * Check whether the last parameter of this method is of type VaList
     * @return true when the last parameter of this method is of type VaList
     */
    public boolean hasVaListParameter() {
        if (parameters == null)
            return false;
        var lastParam = parameters.parameterList.get(parameters.parameterList.size() - 1);
        return lastParam.type != null && "VaList".equals(lastParam.type.simpleJavaType);
    }

    @Override
    public GirElement getParent() {
        return parent;
    }
    
    @Override
    public Parameters getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Parameters ps) {
        this.parameters = ps;
    }

    @Override
    public ReturnValue getReturnValue() {
        return returnValue;
    }

    @Override
    public void setReturnValue(ReturnValue rv) {
        this.returnValue = rv;
    }

    @Override
    public Doc getDoc() {
        return doc;
    }

    @Override
    public String getThrows() {
        return throws_;
    }

    // Log the method call
    protected void log(String cIdentifier, SourceWriter writer) throws IOException {
        if (! Settings.DEBUG_LOGGING_ENABLED)
            return;

        // Prevent stack overflow: don't log the log invocation
        if ("g_log".equals(cIdentifier)
                || "g_strdup_value_contents".equals(cIdentifier)
                || "g_type_name".equals(cIdentifier))
            return;

        var wildcards = new StringBuilder();
        var sw = new SourceWriter(new StringWriter());
        if (parameters != null) {
            boolean first = true;
            for (Parameter p : parameters.parameterList) {
                if (p.isDestroyNotifyParameter() || p.isArrayLengthParameter() || p.isUserDataParameter() || p.isErrorParameter())
                    continue;
                if (!first) sw.write(", ");
                sw.write("\"\"+");
                if (p instanceof InstanceParameter) {
                    sw.write("this");
                } else if (p.type != null && "GType".equals(p.type.cType)) {
                    sw.write("\"GType:\" + org.gnome.gobject.GObjects.typeName(");
                    p.writeName(sw);
                    sw.write(")");
                } else if (p.array != null && (!p.isOutParameter())) {
                    sw.write("java.util.Arrays.toString(");
                    p.writeName(sw);
                    sw.write(")");
                } else {
                    p.writeName(sw);
                }
                if (!first) wildcards.append(", ");
                wildcards.append("%s");
                first = false;
            }
        }
        if (wildcards.isEmpty())
            writer.write("GLibLogger.debug(\"" + cIdentifier + "()\\n\");\n");
        else
            writer.write("GLibLogger.debug(\"" + cIdentifier + "(" + wildcards + ")\\n\".formatted(" + sw + "));\n");
    }
}
