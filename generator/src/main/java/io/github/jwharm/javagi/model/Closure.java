package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.Writer;

public interface Closure extends CallableType {

    default void generateFunctionalInterface(Writer writer, String javaName, int tabs) throws IOException {
        ReturnValue returnValue = getReturnValue();
        Parameters parameters = getParameters();
        String indent = " ".repeat(tabs * 4);
        boolean isVoid = returnValue.type == null || "void".equals(returnValue.type.simpleJavaType);

        writer.write(indent + "@FunctionalInterface\n");
        writer.write(indent + "public interface " + javaName + " {\n");

        // Generate run(...)
        writer.write(indent + "    ");
        returnValue.writeType(writer, false);
        writer.write(" run(");
        if (parameters != null) {
            boolean first = true;
            for (Parameter p : parameters.parameterList) {
                if (p.isUserDataParameter() || p.signalSource) {
                    continue;
                }
                if (!first) writer.write(", ");
                first = false;
                p.writeTypeAndName(writer, true);
            }
        }
        writer.write(");\n");
        writer.write("\n");

        // Generate upcall(...)
        String returnType = isVoid ? "void" : Conversions.toPanamaJavaType(returnValue.type);
        if ("MemoryAddress".equals(returnType))
            returnType = "Addressable";
        writer.write(indent + "    @ApiStatus.Internal default " + returnType + " upcall(");
        if (parameters != null) {
            boolean first = true;
            for (Parameter p : parameters.parameterList) {
                if (!first) writer.write(", ");
                first = false;
                writer.write(Conversions.toPanamaJavaType(p.type) + " " + p.name);
            }
        }
        writer.write(") {\n");

        // Generate preprocessing statements
        if (parameters != null) {
            parameters.generateUpcallPreprocessing(writer, tabs + 2);
        }

        // Call run()
        writer.write(indent + "        ");
        if (!isVoid) writer.write("var RESULT = ");
        writer.write("run(");
        if (parameters != null) {
            parameters.marshalNativeToJava(writer);
        }
        writer.write(");\n");

        // Generate postprocessing statements
        if (parameters != null) {
            parameters.generateUpcallPostprocessing(writer, tabs + 2);
        }

        // Return statement
        if (!isVoid) {
            writer.write(indent + "        return ");
            boolean isMemoryAddress = Conversions.toPanamaJavaType(returnValue.type).equals("MemoryAddress");
            if (isMemoryAddress) writer.write("RESULT == null ? MemoryAddress.NULL.address() : (");
            returnValue.marshalJavaToNative(writer, "RESULT", false, false);
            if (isMemoryAddress) writer.write(").address()");
            writer.write(";\n");
        }
        writer.write(indent + "    }\n");
        writer.write(indent + "    \n");

        // Generate fields
        writer.write(indent + "    @ApiStatus.Internal FunctionDescriptor DESCRIPTOR = FunctionDescriptor.");
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
        writer.write(indent + "    @ApiStatus.Internal MethodHandle HANDLE = Interop.getHandle(MethodHandles.lookup(), " + javaName + ".class, DESCRIPTOR);\n");
        writer.write(indent + "    \n");

        // Generate toCallback()
        writer.write(indent + "    default MemoryAddress toCallback() {\n");
        writer.write(indent + "        return Linker.nativeLinker().upcallStub(HANDLE.bindTo(this), DESCRIPTOR, Interop.getScope()).address();\n");
        writer.write(indent + "    }\n");
        writer.write(indent + "}\n");
    }
}
