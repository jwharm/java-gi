package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.BindingsGenerator;
import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class Signal extends Method {

    public final String when;

    public Signal(GirElement parent, String name, String when, String deprecated, String throws_) {
        super(parent, name, null, deprecated, throws_);
        this.when = when;
    }

    public void generate(Writer writer, boolean isDefault) throws IOException {
        final String signalName = Conversions.toSimpleJavaType(name);
        final String className = ((RegisteredType) parent).javaName;
        final String callbackName = "signal" + className + signalName;
        final boolean returnsBool = returnValue != null && returnValue.type.simpleJavaType.equals("boolean");

        generateFunctionalInterface(writer, signalName, className, returnsBool);
        generateSignalDeclaration(writer, signalName, callbackName, returnsBool, isDefault);
        generateStaticCallback(signalName, className, callbackName, returnsBool, isDefault);
    }

    // Generate the functional interface
    private void generateFunctionalInterface(Writer writer, String signalName, String className, boolean returnsBool) throws IOException {
        writer.write("    @FunctionalInterface\n");
        writer.write("    public interface " + signalName + "Handler {\n");
        writer.write("        " + (returnsBool ? "boolean" : "void") + " signalReceived(" + className + " source");

        if (parameters != null) {
            writer.write(", ");
            parameters.generateJavaParameters(writer);
        }
        writer.write(");\n");

        writer.write("    }\n");
        writer.write("    \n");
    }

    // Generate the static callback method, that will run the handler method.
    private void generateStaticCallback(String signalName, String className, String callbackName, boolean returnsBool, boolean isDefault) throws IOException {
        StringWriter sw = new StringWriter();

        String implClassName = className;
        if (isDefault) {
            implClassName = className + "." + className + "Impl";
        }

        sw.write("    public static " + (returnsBool ? "boolean " : "void ") + callbackName + "(MemoryAddress source");

        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
                sw.write (", " + Conversions.toPanamaJavaType(p.type));
                sw.write(" " + Conversions.toLowerCaseJavaName(p.name));
            }
        }
        sw.write(", MemoryAddress data) {\n");

        sw.write("        int hash = data.get(C_INT, 0);\n");
        sw.write("        var handler = (" + className + "." + signalName + "Handler) Interop.signalRegistry.get(hash);\n");
        sw.write("        " + (returnsBool ? "return " : "") + "handler.signalReceived(new " + implClassName + "(References.get(source))");

        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
                sw.write(", ");
                p.generateCallbackInterop(sw);
            }
        }
        sw.write(");\n");

        sw.write("    }\n");
        sw.write("    \n");
        BindingsGenerator.signalCallbackFunctions.append(sw);
    }

    // Generate the method that connects the signal to the handler (defined by the functional interface above)
    private void generateSignalDeclaration(Writer writer, String signalName, String callbackName, boolean returnsBool, boolean isDefault) throws IOException {
        if (doc != null) {
            doc.generate(writer, 1);
        }
        writer.write("    public " + (isDefault ? "default " : "") + "SignalHandle on" + signalName + "(" + signalName + "Handler handler) {\n");
        writer.write("        try {\n");
        writer.write("            int hash = Interop.registerCallback(handler.hashCode(), handler);\n");
        writer.write("            MemorySegment intSegment = Interop.getAllocator().allocate(C_INT, hash);\n");
        writer.write("            MethodType methodType = MethodType.methodType(");

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
        writer.write(", MemoryAddress.class);\n");

        writer.write("            MethodHandle methodHandle = MethodHandles.lookup().findStatic(JVMCallbacks.class, \"" + callbackName + "\", methodType);\n");
        writer.write("            FunctionDescriptor descriptor = FunctionDescriptor.");

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
        writer.write(", ValueLayout.ADDRESS);\n");

        writer.write("            NativeSymbol nativeSymbol = CLinker.systemCLinker().upcallStub(methodHandle, descriptor, Interop.getScope());\n");
        writer.write("            long handlerId = gtk_h.g_signal_connect_data(handle(), Interop.allocateNativeString(\"" + name + "\").handle(), nativeSymbol, intSegment, MemoryAddress.NULL, 0);\n");
        writer.write("            return new SignalHandle(handle(), handlerId);\n");

        // NoSuchMethodException, IllegalAccessException from findStatic()
        // When the static callback methods have been successfully generated, these exceptions should never happen.
        // We can try to suppress them, but I think it's better to be upfront when they occur, and just crash
        // immediately so the stack trace will be helpful to solve the issue.
        writer.write("        } catch (Exception e) {\n");
        writer.write("            throw new RuntimeException(e);\n");
        writer.write("        }\n");
        writer.write("    }\n");
        writer.write("    \n");
    }
}
