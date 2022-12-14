package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;

public interface Closure extends CallableType {

    default void generateFunctionalInterface(SourceWriter writer, String javaName) throws IOException {
        ReturnValue returnValue = getReturnValue();
        Parameters parameters = getParameters();
        boolean isVoid = returnValue.type == null || "void".equals(returnValue.type.simpleJavaType);

        writer.write("/**\n");
        writer.write(" * Functional interface declaration of the {@code " + javaName + "} callback.\n");
        writer.write(" */\n");
        writer.write("@FunctionalInterface\n");
        writer.write("public interface " + javaName + " {\n");
        writer.write("\n");
        writer.increaseIndent();

        // Generate javadoc for run(...)
        Doc doc = getDoc();
        if (doc != null)
            doc.generate(writer, false);

        // Generate run(...) method
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
        writer.write("@ApiStatus.Internal default " + returnType + " upcall(");
        if (parameters != null) {
            boolean first = true;
            for (Parameter p : parameters.parameterList) {
                if (!first) writer.write(", ");
                first = false;
                writer.write(Conversions.toPanamaJavaType(p.type) + " " + p.name);
            }
        }
        writer.write(") {\n");
        writer.increaseIndent();

        // Is memory allocated?
        boolean hasScope = allocatesMemory();
        if (hasScope) {
            writer.write("try (MemorySession SCOPE = MemorySession.openConfined()) {\n");
            writer.increaseIndent();
        }

        // Generate preprocessing statements
        if (parameters != null) {
            parameters.generateUpcallPreprocessing(writer);
        }

        // Call run()
        if (!isVoid) writer.write("var RESULT = ");
        writer.write("run(");
        if (parameters != null) {
            parameters.marshalNativeToJava(writer);
        }
        writer.write(");\n");

        // Generate postprocessing statements
        if (parameters != null) {
            parameters.generateUpcallPostprocessing(writer);
        }

        boolean isMemoryAddress = !isVoid && Conversions.toPanamaJavaType(returnValue.type).equals("MemoryAddress");
        boolean isNullable = isMemoryAddress && returnValue.nullable;

        if (isNullable) {
            writer.write("if (RESULT != null) {\n");
            writer.increaseIndent();
        }

        // If the return value is a proxy object with transfer-ownership="full", we don't need to unref it anymore.
        if (returnValue.isProxy() && "full".equals(returnValue.transferOwnership)) {
            writer.write("RESULT.yieldOwnership();\n");
        }

        // Return statement
        if (!isVoid) {
            writer.write("return ");

            if (isMemoryAddress) writer.write("(");
            returnValue.marshalJavaToNative(writer, "RESULT", false, false);
            if (isMemoryAddress) writer.write(").address()");
            writer.write(";\n");
            if (isNullable) {
                writer.decreaseIndent();
                writer.write("} else return null;\n");
            }
        }
        if (hasScope) {
            writer.decreaseIndent();
            writer.write("}\n");
        }
        writer.decreaseIndent();
        writer.write("}\n");
        writer.write("\n");

        // Generate fields
        writer.write("/**\n");
        writer.write(" * Describes the parameter types of the native callback function.\n");
        writer.write(" */\n");
        writer.write("@ApiStatus.Internal FunctionDescriptor DESCRIPTOR = FunctionDescriptor.");
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
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * The method handle for the callback.\n");
        writer.write(" */\n");
        writer.write("@ApiStatus.Internal MethodHandle HANDLE = Interop.getHandle(MethodHandles.lookup(), " + javaName + ".class, DESCRIPTOR);\n");
        writer.write("\n");

        // Generate toCallback()
        writer.write("/**\n");
        writer.write(" * Creates a callback that can be called from native code and executes the {@code run} method.\n");
        writer.write(" * @return the memory address of the callback function\n");
        writer.write(" */\n");
        writer.write("default MemoryAddress toCallback() {\n");
        writer.write("    return Linker.nativeLinker().upcallStub(HANDLE.bindTo(this), DESCRIPTOR, MemorySession.global()).address();\n");
        writer.write("}\n");

        writer.decreaseIndent();
        writer.write("}\n");
    }
}
