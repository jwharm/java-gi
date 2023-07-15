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

        // Check for unsupported platforms
        generatePlatformCheck(writer);

        // Generate try-with-resources?
        boolean hasScope = allocatesMemory();
        if (hasScope) {
            writer.write("try (Arena _arena = Arena.openConfined()) {\n");
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
        String panamaReturnType = Conversions.getCarrierType(getReturnValue().type);
        if (! (returnValue.type != null && returnValue.type.isVoid())) {
            writer.write(panamaReturnType + " _result;\n");
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
        writer.write("MemorySegment _func = ((ProxyInstance) this).callParent()\n");
        writer.increaseIndent();
        writer.write("? Interop.lookupVirtualMethodParent(handle(), " + classStruct.javaName + ".getMemoryLayout(), \"" + name + "\"");
        if (parent instanceof Interface) {
            writer.write(", " + className + ".getType()");
        }
        writer.write(")\n");
        writer.write(": Interop.lookupVirtualMethod(handle(), " + classStruct.javaName + ".getMemoryLayout(), \"" + name + "\"");
        if (parent instanceof Interface) {
            writer.write(", " + className + ".getType()");
        }
        writer.write(");\n");
        writer.decreaseIndent();

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

        // Generate the return type
        if (! (returnValue.type != null && returnValue.type.isVoid())) {
            writer.write("_result = (");
            writer.write(panamaReturnType);
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
        writer.write("    throw new AssertionError(\"Unexpected exception occured: \", _err);\n");
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
