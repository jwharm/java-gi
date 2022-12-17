package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.Writer;

public class Callback extends RegisteredType implements CallableType {

    public ReturnValue returnValue;
    public Parameters parameters;

    public Callback(GirElement parent, String name, String cType, String version) {
        super(parent, name, null, cType, version);
    }

    public void generate(Writer writer) throws IOException {
        generateFunctionalInterface(writer);
    }

    private void generateFunctionalInterface(Writer writer) throws IOException {
        boolean isVoid = returnValue.type == null || "void".equals(returnValue.type.simpleJavaType);
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);
        
        writer.write("@FunctionalInterface\n");
        writer.write("public interface " + javaName + " {\n");
        // Generate run(...)
        writer.write("    ");
        writer.write(isVoid ? "void" : returnValue.type.qualifiedJavaType);
        writer.write(" run(");
        if (parameters != null) {
            boolean first = true;
            for (Parameter p : parameters.parameterList) {
                if (!first) writer.write(", ");
                first = false;
                p.generateTypeAndName(writer, true);
            }
        }
        writer.write(");\n");
        writer.write("\n");
        if (parameters != null && parameters.parameterList.stream().anyMatch(Parameter::isOutParameter)) {
            writer.write("    // This callback is NOT supported as it contains an Out parameter!\n");
            writer.write("    default MemoryAddress toCallback() {\n");
            writer.write("        throw new UnsupportedOperationException(\"Operation not supported yet\");\n");
            writer.write("    }\n");
            writer.write("}\n");
            return;
        }
        // Generate upcall(...)
        writer.write("    @ApiStatus.Internal default ");
        writer.write(isVoid ? "void" : Conversions.toPanamaJavaType(returnValue.type));
        writer.write(" upcall(");
        if (parameters != null) {
            boolean first = true;
            for (Parameter p : parameters.parameterList) {
                if (!first) writer.write(", ");
                first = false;
                writer.write(Conversions.toPanamaJavaType(p.type) + " " + p.name);
            }
        }
        writer.write(") {\n");
        writer.write("        ");
        if (!isVoid) writer.write("var RESULT = ");
        writer.write("run(");
        if (parameters != null) {
            boolean first = true;
            for (Parameter p : parameters.parameterList) {
                if (!first) writer.write(", ");
                first = false;
                p.generateReverseInterop(writer, p.name, true);
            }
        }
        writer.write(");\n");
        if (!isVoid) {
            writer.write("        return ");
            boolean isMemoryAddress = Conversions.toPanamaJavaType(returnValue.type).equals("MemoryAddress");
            if (isMemoryAddress) writer.write("(");
            returnValue.generateInterop(writer, "RESULT", false);
            if (isMemoryAddress) writer.write(").address()");
            writer.write(";\n");
        }
        writer.write("    }\n");
        writer.write("    \n");
        // Generate fields
        writer.write("    @ApiStatus.Internal FunctionDescriptor DESCRIPTOR = FunctionDescriptor.");
        if (isVoid) {
            writer.write("ofVoid(");
        } else {
            writer.write("of(");
            writer.write(Conversions.toPanamaMemoryLayout(returnValue.type));
            if (parameters != null) {
                writer.write(", ");
            }
        }
        if (parameters != null) {
            boolean first = true;
            for (Parameter p : parameters.parameterList) {
                if (!first) writer.write(", ");
                first = false;
                writer.write(Conversions.toPanamaMemoryLayout(p.type));
            }
        }
        writer.write(");\n");
        writer.write("    @ApiStatus.Internal MethodHandle HANDLE = Interop.getHandle(" + javaName + ".class, DESCRIPTOR);\n");
        writer.write("    \n");
        // Generate toCallback()
        writer.write("    default MemoryAddress toCallback() {\n");
        writer.write("        return Linker.nativeLinker().upcallStub(HANDLE.bindTo(this), DESCRIPTOR, Interop.getScope()).address();\n");
        writer.write("    }\n");
        writer.write("}\n");
    }

    @Override
    public String getInteropString(String paramName, boolean isPointer, String transferOwnership) {
        return "(Addressable) " + paramName + ".toCallback()";
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
