package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.Writer;

public class Signal extends Method {

    public final String when;
    
    private String signalName, className, callbackName;
    private boolean returnsBool;

    public Signal(GirElement parent, String name, String when, String deprecated, String throws_) {
        super(parent, name, null, deprecated, throws_);
        this.when = when;
    }

    public void generate(Writer writer, boolean isDefault) throws IOException {
        signalName = Conversions.toSimpleJavaType(name);
        className = ((RegisteredType) parent).javaName;
        callbackName = "signal" + className + signalName;
        returnsBool = returnValue != null && returnValue.type.simpleJavaType.equals("boolean");

        generateFunctionalInterface(writer);
        generateSignalDeclaration(writer, isDefault);
    }
    
    // Generate the functional interface
    private void generateFunctionalInterface(Writer writer) throws IOException {
        writer.write("    \n");
        writer.write("    @FunctionalInterface\n");
        writer.write("    public interface " + signalName + "Handler {\n");
        writer.write("        " + (returnsBool ? "boolean" : "void") + " signalReceived(" + className + " source");

        if (parameters != null) {
            writer.write(", ");
            parameters.generateJavaParameters(writer, false);
        }
        writer.write(");\n");

        writer.write("    }\n");
    }

    // Generate the static callback method, that will run the handler method.
    public void generateStaticCallback(Writer writer, boolean isDefault) throws IOException {
        String implClassName = className;
        if (isDefault) {
            implClassName = className + "." + className + "Impl";
        }

        writer.write("        \n");
        writer.write("        public static " + (returnsBool ? "boolean " : "void ") + callbackName + "(MemoryAddress source");

        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
                writer.write (", " + Conversions.toPanamaJavaType(p.type));
                writer.write(" " + Conversions.toLowerCaseJavaName(p.name));
            }
        }
        writer.write(", MemoryAddress data) {\n");

        writer.write("            int HASH = data.get(ValueLayout.JAVA_INT, 0);\n");
        writer.write("            var HANDLER = (" + className + "." + signalName + "Handler) Interop.signalRegistry.get(HASH);\n");
        writer.write("            " + (returnsBool ? "return " : "") + "HANDLER.signalReceived(new " + implClassName + "(Refcounted.get(source))");

        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
                writer.write(", ");
                p.generateReverseInterop(writer, p.name, true);
            }
        }
        writer.write(");\n");

        writer.write("        }\n");
    }

    // Generate the method that connects the signal to the handler (defined by the functional interface above)
    private void generateSignalDeclaration(Writer writer, boolean isDefault) throws IOException {
        writer.write("    \n");
        if (doc != null) {
            doc.generate(writer, 1);
        }
        writer.write("    public " + (isDefault ? "default " : "") + "SignalHandle on" + signalName + "(" + signalName + "Handler handler) {\n");
        writer.write("        try {\n");
        writer.write("            var RESULT = (long) Interop.g_signal_connect_data.invokeExact(\n");
        writer.write("                handle(),\n");
        writer.write("                Interop.allocateNativeString(\"" + name + "\"),\n");
        writer.write("                (Addressable) Linker.nativeLinker().upcallStub(\n");
        writer.write("                    MethodHandles.lookup().findStatic(" + className + ".Callbacks.class, \"" + callbackName + "\",\n");
        writer.write("                        MethodType.methodType(");
        if (returnsBool) {
            writer.write("boolean.class, MemoryAddress.class");
        } else {
            writer.write("void.class, MemoryAddress.class");
        }
        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
                writer.write(", " + Conversions.toPanamaJavaType(p.type) + ".class");
            }
        }
        writer.write(", MemoryAddress.class)),\n");
        writer.write("                    FunctionDescriptor.");
        if (returnsBool) {
            writer.write("of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS");
        } else {
            writer.write("ofVoid(ValueLayout.ADDRESS");
        }
        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
                writer.write(", " + Conversions.toPanamaMemoryLayout(p.type));
            }
        }
        writer.write(", ValueLayout.ADDRESS),\n");
        writer.write("                    Interop.getScope()),\n");
        writer.write("                (Addressable) Interop.getAllocator().allocate(ValueLayout.JAVA_INT, Interop.registerCallback(handler)),\n");
        writer.write("                (Addressable) MemoryAddress.NULL, 0);\n");
        writer.write("            return new SignalHandle(handle(), RESULT);\n");
        writer.write("        } catch (Throwable ERR) {\n");
        writer.write("            throw new AssertionError(\"Unexpected exception occured: \", ERR);\n");
        writer.write("        }\n");
        writer.write("    }\n");
    }
}
