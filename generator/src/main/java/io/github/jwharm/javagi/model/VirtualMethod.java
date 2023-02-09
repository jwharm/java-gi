package io.github.jwharm.javagi.model;

import java.io.IOException;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

public class VirtualMethod extends Method {

    public VirtualMethod(GirElement parent, String name, String deprecated, String throws_) {
        super(parent, name, null, deprecated, throws_, null, null, null);
    }
    
    public void generate(SourceWriter writer) throws IOException {
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

        // Generate try-with-resources?
        boolean hasScope = allocatesMemory();
        if (hasScope) {
            writer.write("try (MemorySession SCOPE = MemorySession.openConfined()) {\n");
            writer.increaseIndent();
        }

        // Generate preprocessing statements for all parameters
        if (parameters != null) {
            parameters.generatePreprocessing(writer);
        }

        // Allocate GError pointer
        if (throws_ != null) {
            writer.write("MemorySegment GERROR = SCOPE.allocate(Interop.valueLayout.ADDRESS);\n");
        }
        
        // Variable declaration for return value
        String panamaReturnType = Conversions.toPanamaJavaType(getReturnValue().type);
        if (! (returnValue.type != null && returnValue.type.isVoid())) {
            writer.write(panamaReturnType + " RESULT;\n");
        }
        
        // The method call is wrapped in a try-catch block
        writer.write("try {\n");
        writer.increaseIndent();
        
        Record classStruct = null;
        if (parent instanceof Class c) {
            classStruct = c.classStruct;
        } else if (parent instanceof Interface i) {
            classStruct = i.classStruct;
        }
        if (classStruct == null) {
            throw new IOException("Cannot find class struct for " + parent.name);
        }
        writer.write("long _offset = " + classStruct.javaName);
        writer.write(".getMemoryLayout().byteOffset(MemoryLayout.PathElement.groupElement(\"");
        writer.write(name);
        writer.write("\"));\n");
        
        writer.write("FunctionDescriptor _fdesc = ");
        boolean varargs = generateFunctionDescriptor(writer);
        writer.write(";\n");
        
        // Generate the return type
        if (! (returnValue.type != null && returnValue.type.isVoid())) {
            writer.write("RESULT = (");
            writer.write(panamaReturnType);
            writer.write(") ");
        }

        // Invoke to the method handle
        writer.write("Interop.downcallHandle(((MemoryAddress) handle()).addOffset(_offset), _fdesc).invokeExact");
        
        // Marshall the parameters to the native types
        if (parameters != null) {
            writer.write("(");
            parameters.marshalJavaToNative(writer, throws_);
            writer.write(")");
        } else {
            writer.write("()");
        }
        writer.write(";\n");
        
        // If something goes wrong in the invokeExact() call
        writer.decreaseIndent();
        writer.write("} catch (Throwable ERR) {\n");
        writer.write("    throw new AssertionError(\"Unexpected exception occured: \", ERR);\n");
        writer.write("}\n");

        // Throw GErrorException
        if (throws_ != null) {
            writer.write("if (GErrorException.isErrorSet(GERROR)) {\n");
            writer.write("    throw new GErrorException(GERROR);\n");
            writer.write("}\n");
        }
        
        // Generate post-processing actions for parameters
        if (parameters != null) {
            parameters.generatePostprocessing(writer);
        }

        // Generate a call to "yieldOwnership" to disable the Cleaner
        // when a user manually calls "unref"
        if (name.equals("unref")) {
            writer.write("this.yieldOwnership();\n");
        }
        
        // Generate code to process and return the result value
        returnValue.generate(writer, panamaReturnType);

        // End of memory allocation scope
        if (hasScope) {
            writer.decreaseIndent();
            writer.write("}\n");
        }

        writer.decreaseIndent();
        writer.write("}\n");
    }
}
