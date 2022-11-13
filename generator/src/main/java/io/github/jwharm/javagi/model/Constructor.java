package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.Writer;

public class Constructor extends Method {

    public Constructor(GirElement parent, String name, String cIdentifier, String deprecated, String throws_) {
        super(parent, name, cIdentifier, deprecated, throws_);
    }

    public void generate(Writer writer, boolean isInterface) throws IOException {
        // Do not generate deprecated constructors.
        if ("1".equals(deprecated)) {
            return;
        }

        String privateMethodName = generateConstructorHelper(writer);

        writer.write("    \n");
        if (doc != null) {
            doc.generate(writer, 1);
        }
        
        if ("1".equals(deprecated)) {
            writer.write("    @Deprecated\n");
        }
        
        writer.write("    public ");
        writer.write(((RegisteredType) parent).javaName);
        if (parameters != null) {
            writer.write("(");
            parameters.generateJavaParameters(writer, false);
            writer.write(")");
        } else {
            writer.write("()");
        }
        if (throws_ != null) {
            writer.write(" throws GErrorException");
        }
        writer.write(" {\n");
        
        if (! isSafeToBind()) {
            writer.write("        this(null, null); // avoid compiler error\n");
            writer.write("        throw new UnsupportedOperationException(\"Operation not supported yet\");\n");
            writer.write("    }\n");
            return;
        }
        
        writer.write("        super(" + privateMethodName);
        if (parameters != null) {
            writer.write("(");
            parameters.generateJavaParameterNames(writer);
            writer.write(")");
        } else {
            writer.write("()");
        }
        writer.write(", " + returnValue.transferOwnership() + ");\n");
        writer.write("    }\n");
    }

    public void generateNamed(Writer writer, boolean isInterface) throws IOException {
        RegisteredType clazz = (RegisteredType) parent;

        String privateMethodName = generateConstructorHelper(writer);

        writer.write("    \n");
        if (doc != null) {
            doc.generate(writer, 1);
        }
        
        if ("1".equals(deprecated)) {
            writer.write("    @Deprecated\n");
        }
        
        writer.write("    public static " + clazz.javaName + " " + Conversions.toLowerCaseJavaName(name));
        if (parameters != null) {
            writer.write("(");
            parameters.generateJavaParameters(writer, false);
            writer.write(")");
        } else {
            writer.write("()");
        }
        if (throws_ != null) {
            writer.write(" throws GErrorException");
        }
        writer.write(" {\n");
        
        if (! isSafeToBind()) {
            writer.write("        throw new UnsupportedOperationException(\"Operation not supported yet\");\n");
            writer.write("    }\n");
            return;
        }
        
        writer.write("        return new " + clazz.javaName + "(" + privateMethodName);
        if (parameters != null) {
            writer.write("(");
            parameters.generateJavaParameterNames(writer);
            writer.write(")");
        } else {
            writer.write("()");
        }
        writer.write(", " + returnValue.transferOwnership() + ");\n");
        writer.write("    }\n");
    }

    // Because constructors sometimes throw exceptions, we need to allocate a GError segment before
    // calling "super(..., GERROR)", which is not allowed - the super() call must
    // be the first statement in the constructor. Therefore, we always generate a private method that
    // prepares a GError memorysegment if necessary, calls the C API and throws the GErrorException
    // (if necessary). The "real" constructor just calls super(private_method());
    private String generateConstructorHelper(Writer writer) throws IOException {
        String methodName = "construct" + Conversions.toCamelCase(name, true);
        writer.write("    \n");
        writer.write("    private static Addressable " + methodName);
        if (parameters != null) {
            writer.write("(");
            parameters.generateJavaParameters(writer, false);
            writer.write(")");
        } else {
            writer.write("()");
        }
        if (throws_ != null) {
            writer.write(" throws GErrorException");
        }
        writer.write(" {\n");
        
        if (! isSafeToBind()) {
            writer.write("        throw new UnsupportedOperationException(\"Operation not supported yet\");\n");
            writer.write("    }\n");
            return methodName;
        }
        
        // Generate checks for null parameters
        generateNullParameterChecks(writer);
        
        if (throws_ != null) {
            writer.write("        MemorySegment GERROR = Interop.getAllocator().allocate(ValueLayout.ADDRESS);\n");
        }
        
        writer.write("        Addressable RESULT;\n");
        writer.write("        try {\n");
        writer.write("            RESULT = (MemoryAddress) DowncallHandles." + cIdentifier + ".invokeExact");
        if (parameters != null) {
            writer.write("(");
            parameters.generateCParameters(writer, throws_);
            writer.write(")");
        } else {
            writer.write("()");
        }
        writer.write(";\n");
        writer.write("        } catch (Throwable ERR) {\n");
        writer.write("            throw new AssertionError(\"Unexpected exception occured: \", ERR);\n");
        writer.write("        }\n");
        // If the parameter has attribute transfer-ownership="full", we don't need to unref it anymore.
        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
                // Only for proxy objects where ownership is fully transferred away, 
                // unless it's an out parameter or a pointer
                if (p.isProxy()
                        && "full".equals(p.transferOwnership) 
                        && (! p.isOutParameter()) 
                        && (p.type.cType == null || (! p.type.cType.endsWith("**")))) {
                    writer.write("        " + (p.isInstanceParameter() ? "this" : p.name) + ".yieldOwnership();\n");
                }
            }
        }
        if (throws_ != null) {
            writer.write("        if (GErrorException.isErrorSet(GERROR)) {\n");
            writer.write("            throw new GErrorException(GERROR);\n");
            writer.write("        }\n");
        }
        writer.write("        return RESULT;\n");
        writer.write("    }\n");
        return methodName;
    }
}
