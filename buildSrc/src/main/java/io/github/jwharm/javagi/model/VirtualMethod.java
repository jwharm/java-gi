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

public class VirtualMethod extends Method {

    public Method linkedMethod = null;

    public VirtualMethod(GirElement parent, String name, String deprecated, String throws_) {
        super(parent, name, null, deprecated, throws_, null, null, null);
        visibility = "protected";
    }
    
    public void generate(SourceWriter writer) throws IOException {
        if (parent instanceof Interface || linkedMethod != null) {
            return;
        }

        writer.write("\n");
        
        // Documentation
        if (doc != null) {
            doc.generate(writer, false);
        }

        // Deprecation
        if ("1".equals(deprecated)) {
            writer.write("@Deprecated\n");
        }

        // Declaration
        writer.write(getMethodDeclaration());
        
        writer.write(" {\n");
        writer.increaseIndent();

        // Check for unsupported platforms
        generatePlatformCheck(writer);

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
        
        Record classStruct = null;
        String className = null;
        if (parent instanceof Class c) {
            classStruct = c.classStruct;
            className = c.javaName;
        } else if (parent instanceof Interface i) {
            classStruct = i.classStruct;
            className = i.javaName;
        }
        if (classStruct == null) {
            throw new IOException("Cannot find typestruct for " + parent.name);
        }
        writer.write("MemorySegment _func = Overrides.lookupVirtualMethodParent(handle(), " + classStruct.javaName + ".getMemoryLayout(), \"" + name + "\"");
        if (parent instanceof Interface) {
            writer.write(", " + className + ".getType()");
        }
        writer.write(");\n");
        writer.write("if (MemorySegment.NULL.equals(_func)) {\n");
        writer.write("    throw new NullPointerException();\n");
        writer.write("}\n");

        // Check if the virtual method points to a null address.
        writer.write("FunctionDescriptor _fdesc = ");
        generateFunctionDescriptor(writer);
        writer.write(";\n");

        // The method call is wrapped in a try-catch block
        writer.write("try {\n");
        writer.increaseIndent();

        // Log the method call
        log(classStruct.javaName + "." + name, writer);

        // Generate the return type
        if (! (returnValue.type != null && returnValue.type.isVoid())) {
            writer.write("_result = (");
            writer.write(carrierType);
            writer.write(") ");
        }

        // Invoke to the method handle
        writer.write("Interop.downcallHandle(_func, _fdesc).invokeExact(");
        
        // Marshall the parameters to the native types
        if (parameters != null) {
            parameters.marshalJavaToNative(writer, throws_);
        }
        writer.write(");\n");
        
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
        returnValue.generate(writer);

        // End of memory allocation scope
        if (hasScope) {
            writer.decreaseIndent();
            writer.write("}\n");
        }

        writer.decreaseIndent();
        writer.write("}\n");
    }
}
