package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;

public interface Closure extends CallableType {

    default void generateFunctionalInterface(SourceWriter writer, String javaName) throws IOException {
        ReturnValue returnValue = getReturnValue();
        Parameters parameters = getParameters();
        boolean isVoid = returnValue.type == null || "void".equals(returnValue.type.simpleJavaType);

        if (getDoc() == null) {
            writer.write("/**\n");
            writer.write(" * Functional interface declaration of the {@code " + javaName + "} callback.\n");
            writer.write(" */\n");
        }
        writer.write("@FunctionalInterface\n");
        writer.write("public interface " + javaName + " {\n");
        writer.write("\n");
        writer.increaseIndent();

        // Generate javadoc for run(...)
        Doc doc = getDoc();
        if (doc != null)
            doc.generate(writer, false);

        // Generate run(...) method
        returnValue.writeType(writer, false, true);
        writer.write(" run(");
        if (parameters != null) {
            boolean first = true;
            for (Parameter p : parameters.parameterList) {
                if (p.isUserDataParameter() || p.isDestroyNotifyParameter() || p.isArrayLengthParameter()) {
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
        writer.write("/**\n");
        writer.write(" * The {@code upcall} method is called from native code. The parameters \n");
        writer.write(" * are marshalled and {@link #run} is executed.\n");
        writer.write(" */\n");
        String returnType = isVoid ? "void" : Conversions.toPanamaJavaType(returnValue.type);
        if ("MemoryAddress".equals(returnType))
            returnType = "Addressable";
        writer.write("@ApiStatus.Internal default " + returnType + " upcall(");

        // For signals, the first parameter in the upcall is a pointer to the source.
        // Add it to the upcall function signature.
        boolean first = true;
        if (this instanceof Signal signal) {
            writer.write("MemoryAddress source" + ((RegisteredType) signal.parent).javaName);
            first = false;
        }

        if (parameters != null) {
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
            writer.write("try (MemorySession _scope = MemorySession.openConfined()) {\n");
            writer.increaseIndent();
        }

        // Generate preprocessing statements
        if (parameters != null) {
            parameters.generateUpcallPreprocessing(writer);
        }

        // Call run()
        if (!isVoid) writer.write("var _result = ");
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
        boolean isNullable = isMemoryAddress && (!returnValue.notnull);

        if (isNullable) {
            writer.write("if (_result != null) {\n");
            writer.increaseIndent();
        }

        // If the return value is a proxy object with transfer-ownership="full", the JVM must own a reference.
        if (returnValue.isGObject() && "full".equals(returnValue.transferOwnership)) {
            writer.write("_result.ref();\n");
        }

        // Return statement
        if (!isVoid) {
            writer.write("return ");

            if (isMemoryAddress) writer.write("(");
            returnValue.marshalJavaToNative(writer, "_result", false, false);
            if (isMemoryAddress) writer.write(").address()");
            writer.write(";\n");
            if (isNullable) {
                writer.decreaseIndent();
                writer.write("} else {\n");
                writer.write("    return MemoryAddress.NULL;\n");
                writer.write("}\n");
            }
        }
        if (hasScope) {
            writer.decreaseIndent();
            writer.write("}\n");
        }
        writer.decreaseIndent();
        writer.write("}\n");
        writer.write("\n");

        // Generate toCallback()
        writer.write("/**\n");
        writer.write(" * Creates a callback that can be called from native code and executes the {@code run} method.\n");
        writer.write(" * @return the memory address of the callback function\n");
        writer.write(" */\n");
        writer.write("default MemoryAddress toCallback() {\n");
        writer.increaseIndent();

        // Generate function descriptor
        writer.write("FunctionDescriptor _fdesc = FunctionDescriptor.");
        if (isVoid) {
            writer.write("ofVoid(");
        } else {
            writer.write("of(");
            writer.write(Conversions.toPanamaMemoryLayout(returnValue.type));
            if (parameters != null || this instanceof Signal) {
                writer.write(", ");
            }
        }
        // For signals, add the pointer to the source
        if (this instanceof Signal) {
            writer.write("Interop.valueLayout.ADDRESS");
            if (parameters != null) {
                writer.write(", ");
            }
        }
        if (parameters != null) {
            first = true;
            for (Parameter p : parameters.parameterList) {
                if (!first) writer.write(", ");
                first = false;
                writer.write(Conversions.toPanamaMemoryLayout(p.type));
            }
        }
        writer.write(");\n");

        // Generate method handle
        writer.write("MethodHandle _handle = Interop.upcallHandle(MethodHandles.lookup(), " + javaName + ".class, _fdesc);\n");

        // Create and return upcall stub
        writer.write("return Linker.nativeLinker().upcallStub(_handle.bindTo(this), _fdesc, MemorySession.global()).address();\n");

        writer.decreaseIndent();
        writer.write("}\n");
        writer.decreaseIndent();
        writer.write("}\n");
    }
}
