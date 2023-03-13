package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;

public interface Closure extends CallableType {

    default void generateFunctionalInterface(SourceWriter writer, String javaName) throws IOException {
        ReturnValue returnValue = getReturnValue();
        Parameters parameters = getParameters();
        String throws_ = getThrows();

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
        writer.write(")");
        if (throws_ != null) {
            writer.write(" throws GErrorException");
        }
        writer.write(";\n");
        writer.write("\n");

        // Generate upcall(...)
        writer.write("/**\n");
        writer.write(" * The {@code upcall} method is called from native code. The parameters \n");
        writer.write(" * are marshalled and {@link #run} is executed.\n");
        writer.write(" */\n");
        generateUpcallMethod(writer, "upcall","run");

        // Generate toCallback()
        writer.write("/**\n");
        writer.write(" * Creates a callback that can be called from native code and executes the {@code run} method.\n");
        writer.write(" * @return the memory address of the callback function\n");
        writer.write(" */\n");
        writer.write("default MemoryAddress toCallback() {\n");
        writer.increaseIndent();

        // Generate function descriptor
        writer.write("FunctionDescriptor _fdesc = ");
        generateFunctionDescriptor(writer);
        writer.write(";\n");

        // Generate method handle
        writer.write("MethodHandle _handle = Interop.upcallHandle(MethodHandles.lookup(), " + javaName + ".class, _fdesc);\n");

        // Create and return upcall stub
        writer.write("return Linker.nativeLinker().upcallStub(_handle.bindTo(this), _fdesc, MemorySession.global()).address();\n");

        writer.decreaseIndent();
        writer.write("}\n");
        writer.decreaseIndent();
        writer.write("}\n");
    }

    default void generateUpcallMethod(SourceWriter writer, String name, String methodToInvoke) throws IOException {
        ReturnValue returnValue = getReturnValue();
        Parameters parameters = getParameters();
        boolean isVoid = returnValue.type == null || "void".equals(returnValue.type.simpleJavaType);
        String throws_ = getThrows();

        String returnType = isVoid ? "void" : Conversions.toPanamaJavaType(returnValue.type);
        if ("MemoryAddress".equals(returnType))
            returnType = "Addressable";

        writer.write((methodToInvoke.equals("run") ? "@ApiStatus.Internal default " : "private ") + returnType + " ");
        writer.write(name);
        writer.write("(");

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
        // Add a parameter to write the GError
        if (throws_ != null) {
            if (!first) writer.write(", ");
            writer.write("MemoryAddress _gerrorPointer");
        }

        writer.write(") {\n");
        writer.increaseIndent();

        // Generate try-catch block for reflection calls
        if (methodToInvoke.endsWith("invoke")) {
            writer.write("try {\n");
            writer.increaseIndent();
        }

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

        // Generate try-catch block for exceptions thrown from inside the callback
        if (throws_ != null) {
            writer.write("try {\n");
            writer.increaseIndent();
        }

        if (!isVoid) {
            writer.write("var _result = ");

            // Add cast to reflection call
            if (methodToInvoke.endsWith("invoke")) {
                writer.write("(");
                returnValue.writeType(writer, false, false);
                writer.write(") ");
            }
        }

        // Call run()
        writer.write(methodToInvoke + "(");
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

        if (throws_ != null) {
            writer.decreaseIndent();
            if (methodToInvoke.endsWith("invoke")) {
                writer.write("} catch (java.lang.reflect.InvocationTargetException _ite) {\n");
                writer.increaseIndent();
                writer.write("if (_ite.getCause() instanceof GErrorException _ge) {\n");
            } else {
                writer.write("} catch (GErrorException _ge) {\n");
            }
            writer.increaseIndent();
            writer.write("org.gnome.glib.GError _gerror = (org.gnome.glib.GError) InstanceCache.getForType(_gerrorPointer, org.gnome.glib.GError::new);\n");
            writer.write("_gerror.writeCode(_ge.getCode());\n");
            writer.write("_gerror.writeDomain(_ge.getDomain());\n");
            writer.write("_gerror.writeMessage(_ge.getMessage());\n");
            // Return null
            if (! isVoid) {
                Type type = returnValue.type;
                if (type != null
                        && (type.isPrimitive || type.isEnum() || type.isBitfield() || type.isAliasForPrimitive())
                        && (! type.isPointer())) {
                    writer.write("return 0;\n");
                } else {
                    writer.write("return MemoryAddress.NULL;\n");
                }
            }
            if (methodToInvoke.endsWith("invoke")) {
                writer.decreaseIndent();
                writer.write("} else {\n");
                writer.write("    throw _ite;\n");
                writer.write("}\n");
            }
            writer.decreaseIndent();
            writer.write("}\n");
        }

        if (hasScope) {
            writer.decreaseIndent();
            writer.write("}\n");
        }

        // Close try-catch block for reflection calls
        if (methodToInvoke.endsWith("invoke")) {
            writer.decreaseIndent();
            writer.write("} catch (Exception e) {\n");
            writer.write("    throw new RuntimeException(e);\n");
            writer.write("}\n");
        }
        writer.decreaseIndent();
        writer.write("}\n");
        writer.write("\n");
    }
}
