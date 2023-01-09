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

        writer.write(indent + "/**\n");
        writer.write(indent + " * Functional interface declaration of the {@code " + javaName + "} callback.\n");
        writer.write(indent + " */\n");
        writer.write(indent + "@FunctionalInterface\n");
        writer.write(indent + "public interface " + javaName + " {\n");
        writer.write(indent + "\n");

        // Generate javadoc for run(...)
        Doc doc = getDoc();
        if (doc != null)
            doc.generate(writer, tabs + 1, false);

        // Generate run(...) method
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

        // Is memory allocated?
        boolean hasScope = allocatesMemory();
        if (hasScope) {
            writer.write(indent + "      try (MemorySession SCOPE = MemorySession.openConfined()) {\n");
        }

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

        // If the return value is a proxy object with transfer-ownership="full", we don't need to unref it anymore.
        if (returnValue.isProxy() && "full".equals(returnValue.transferOwnership)) {
            writer.write(indent + "        RESULT.yieldOwnership();\n");
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
        if (hasScope) {
            writer.write(indent + "      }\n");
        }
        writer.write(indent + "    }\n");
        writer.write(indent + "    \n");

        // Generate fields
        writer.write(indent + "    /**\n");
        writer.write(indent + "     * Describes the parameter types of the native callback function.\n");
        writer.write(indent + "     */\n");
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
        writer.write(indent + "    \n");
        writer.write(indent + "    /**\n");
        writer.write(indent + "     * The method handle for the callback.\n");
        writer.write(indent + "     */\n");
        writer.write(indent + "    @ApiStatus.Internal MethodHandle HANDLE = Interop.getHandle(MethodHandles.lookup(), " + javaName + ".class, DESCRIPTOR);\n");
        writer.write(indent + "    \n");

        // Generate toCallback()
        writer.write(indent + "    /**\n");
        writer.write(indent + "     * Creates a callback that can be called from native code and executes the {@code run} method.\n");
        writer.write(indent + "     * @return the memory address of the callback function\n");
        writer.write(indent + "     */\n");
        writer.write(indent + "    default MemoryAddress toCallback() {\n");
        writer.write(indent + "        return Linker.nativeLinker().upcallStub(HANDLE.bindTo(this), DESCRIPTOR, MemorySession.global()).address();\n");
        writer.write(indent + "    }\n");
        writer.write(indent + "}\n");
    }
}
