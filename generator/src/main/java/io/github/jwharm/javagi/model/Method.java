package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

import io.github.jwharm.javagi.generator.Conversions;

public class Method extends GirElement implements CallableType {

    public final String cIdentifier;
    public final String deprecated;
    public final String throws_;
    public final String shadowedBy;
    public final String shadows;
    public ReturnValue returnValue;
    public Parameters parameters;

    public Method(GirElement parent, String name, String cIdentifier, String deprecated,
                  String throws_, String shadowedBy, String shadows) {
        super(parent);
        this.name = shadows == null ? name : shadows; // Language bindings are expected to rename a function to the shadowed function
        this.cIdentifier = cIdentifier;
        this.deprecated = deprecated;
        this.throws_ = throws_;
        this.shadowedBy = shadowedBy;
        this.shadows = shadows;

        // Handle empty names. (For example, GLib.g_iconv is named "".)
        if ("".equals(name)) {
            this.name = cIdentifier;
        }
    }
    
    public void generateMethodHandle(Writer writer, boolean isInterface) throws IOException {
        boolean varargs = false;
        writer.write("        \n");
        writer.write("        ");
        writer.write(isInterface ? "@ApiStatus.Internal\n        " : "private ");
        writer.write("static final MethodHandle " + cIdentifier + " = Interop.downcallHandle(\n");
        writer.write("            \"" + cIdentifier + "\",\n");
        writer.write("            FunctionDescriptor.");
        if (returnValue.type == null || "void".equals(returnValue.type.simpleJavaType)) {
            writer.write("ofVoid(");
        } else {
            writer.write("of(" + Conversions.toPanamaMemoryLayout(returnValue.type));
            if (parameters != null) {
                writer.write(", ");
            }
        }
        if (parameters != null) {
            for (int i = 0; i < parameters.parameterList.size(); i++) {
                if (parameters.parameterList.get(i).varargs) {
                    varargs = true;
                    break;
                }
                if (i > 0) {
                    writer.write(", ");
                }
                writer.write(Conversions.toPanamaMemoryLayout(parameters.parameterList.get(i).type));
            }
        }
        if (throws_ != null) {
            writer.write(", Interop.valueLayout.ADDRESS");
        }
        writer.write("),\n");
        writer.write(varargs ? "            true\n" : "            false\n");
        writer.write("        );\n");
    }

    public void generate(Writer writer, boolean isInterface, boolean isStatic) throws IOException {
        writer.write("    \n");
        
        // Documentation
        if (doc != null) {
            doc.generate(writer, 1);
        }

        // Deprecation
        if ("1".equals(deprecated)) {
            writer.write("    @Deprecated\n");
        }

        if (isInterface && !isStatic) {
            // Default interface methods
            writer.write("    default ");
        } else {
            // Visibility
            writer.write("    public ");
        }

        // Static methods (functions)
        if (isStatic) {
            writer.write("static ");
        }

        // Return type
        getReturnValue().writeType(writer, true);

        // Method name
        String methodName = Conversions.toLowerCaseJavaName(name);
        if (isInterface) { // Overriding toString() in a default method is not allowed.
            methodName = Conversions.replaceJavaObjectMethodNames(methodName);
        }
        writer.write(" ");
        writer.write(methodName);

        // Parameters
        if (getParameters() != null) {
            writer.write("(");
            getParameters().generateJavaParameters(writer, false);
            writer.write(")");
        } else {
            writer.write("()");
        }

        // Exceptions
        if (throws_ != null) {
            writer.write(" throws io.github.jwharm.javagi.GErrorException");
        }
        writer.write(" {\n");
        
        // Generate preprocessing statements for all parameters
        if (parameters != null) {
            parameters.generatePreprocessing(writer, 2);
        }

        // Allocate GError pointer
        if (throws_ != null) {
            writer.write("        MemorySegment GERROR = Interop.getAllocator().allocate(Interop.valueLayout.ADDRESS);\n");
        }
        
        // Variable declaration for return value
        String panamaReturnType = Conversions.toPanamaJavaType(getReturnValue().type);
        if (! (returnValue.type != null && returnValue.type.isVoid())) {
            writer.write("        " + panamaReturnType + " RESULT;\n");
        }
        
        // The method call is wrapped in a try-catch block
        writer.write("        try {\n");
        writer.write("            ");
        
        // Generate the return type
        if (! (returnValue.type != null && returnValue.type.isVoid())) {
            writer.write("RESULT = (");
            writer.write(panamaReturnType);
            writer.write(") ");
        }

        // Invoke to the method handle
        writer.write("DowncallHandles." + cIdentifier + ".invokeExact");
        
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
        writer.write("        } catch (Throwable ERR) {\n");
        writer.write("            throw new AssertionError(\"Unexpected exception occured: \", ERR);\n");
        writer.write("        }\n");

        // Throw GErrorException
        if (throws_ != null) {
            writer.write("        if (GErrorException.isErrorSet(GERROR)) {\n");
            writer.write("            throw new GErrorException(GERROR);\n");
            writer.write("        }\n");
        }
        
        // Generate post-processing actions for parameters
        if (parameters != null) {
            parameters.generatePostprocessing(writer, 2);
        }
        
        // Generate code to process and return the result value
        returnValue.generate(writer, panamaReturnType, 2);
        
        writer.write("    }\n");
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
}
