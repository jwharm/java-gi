package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;
import java.util.stream.Stream;

public class Signal extends Method implements Closure {

    public final String when;
    private final String signalName;
    private final String qualifiedName;
    public boolean detailed;

    public Signal(GirElement parent, String name, String when, String detailed, String deprecated, String throws_) {
        super(parent, name, null, deprecated, throws_, null, null, null);
        this.when = when;
        this.detailed = "1".equals(detailed);

        String className = ((RegisteredType) parent).javaName;
        signalName = Conversions.toSimpleJavaType(name, getNamespace());
        qualifiedName = className + "." + signalName;
    }

    public void generate(SourceWriter writer) throws IOException {
        writer.write("\n");
        generateFunctionalInterface(writer, signalName);
        writer.write("\n");

        // Generate signal connect function
        if (doc != null) {
            doc.generate(writer, true);
        }
        writer.write("public " + (parent instanceof Interface ? "default " : "") + "Signal<" + qualifiedName + "> on" + signalName + "(");
        
        // For detailed signals like GObject.notify::..., generate a String parameter to specify the detailed signal
        if (detailed) {
            writer.write("@Nullable String detail, ");
        }
        
        writer.write(qualifiedName + " handler) {\n");
        if (!isApi()) {
            writer.increaseIndent();

            writer.write("MemorySession _scope = MemorySession.openImplicit();\n");
            writer.write("try {\n");
            writer.write("    var _result = (long) Interop.g_signal_connect_data.invokeExact(\n");
            writer.write("        handle(), Interop.allocateNativeString(\"" + name + "\"");
            if (detailed) {
                writer.write(" + ((detail == null || detail.isBlank()) ? \"\" : (\"::\" + detail))");
            }
            writer.write(", _scope), (Addressable) handler.toCallback(), (Addressable) MemoryAddress.NULL, (Addressable) MemoryAddress.NULL, 0);\n");
            writer.write("    return new Signal<>(handle(), _result);\n");
            writer.write("} catch (Throwable _err) {\n");
            writer.write("    throw new AssertionError(\"Unexpected exception occured: \", _err);\n");
            writer.write("}\n");

            writer.decreaseIndent();
        } else writer.write("    throw Interop.apiError();\n");
        writer.write("}\n");

        // Check if an emit function already is defined in the GIR file
        boolean emitFunctionExists = false;
        for (Method method : Stream.concat(parent.methodList.stream(), parent.functionList.stream()).toList()) {
            String n = Conversions.toLowerCaseJavaName(method.name);
            if (n.equals("emit" + signalName)) emitFunctionExists = true;
        }

        // Generate signal emit function
        if (!emitFunctionExists) {
            writer.write("\n");
            writer.write("/**\n");
            writer.write(" * Emits the \"" + name + "\" signal. See {@link #on" + signalName + "}.\n");
            writer.write(" */\n");
            writer.write("public " + (parent instanceof Interface ? "default " : ""));
            returnValue.writeType(writer, true, true);
            writer.write(" emit" + signalName + "(");
            if (detailed) {
                writer.write("@Nullable String detail");
            }

            if (parameters != null) {
                if (detailed) writer.write(", ");
                parameters.generateJavaParameters(writer, false);
            }

            writer.write(") {\n");

            if (isApi()) {
                writer.write("    throw Interop.apiError();\n");
                writer.write("}\n");
                return;
            }

            writer.increaseIndent();
            writer.write("try (MemorySession _scope = MemorySession.openConfined()) {\n");
            writer.increaseIndent();
            if (parameters != null) {
                parameters.generatePreprocessing(writer);
            }
            boolean hasReturn = returnValue.type != null && !"void".equals(returnValue.type.simpleJavaType);
            if (hasReturn) {
                writer.write("MemorySegment _result = _scope.allocate(" + Conversions.getValueLayout(returnValue.type) + ");\n");
            }
            writer.write("Interop.g_signal_emit_by_name.invokeExact(\n");
            writer.write("        handle(),\n");
            writer.write("        Interop.allocateNativeString(\"" + name + "\"");
            if (detailed) {
                writer.write(" + ((detail == null || detail.isBlank()) ? \"\" : (\"::\" + detail))");
            }
            writer.write(", _scope)");
            if (parameters != null || hasReturn) {
                writer.increaseIndent();
                writer.write(",\n");
                writer.write("    new Object[] {");
                if (parameters != null) {
                    if (parameters.parameterList.size() == 1) {
                        writer.write("\n");
                        writer.write("        ");
                    }
                    parameters.marshalJavaToNative(writer, null);
                }
                if (hasReturn) {
                    writer.write(parameters == null ? "\n" : ",\n");
                    writer.write("        _result.address()");
                }
                writer.write("\n");
                writer.write("    }\n");
                writer.decreaseIndent();
            } else {
                writer.write(", new Object[0]");
            }
            writer.write(");\n");
            if (parameters != null) {
                parameters.generatePostprocessing(writer);
            }
            if (hasReturn) {
                writer.write("return ");
                returnValue.marshalNativeToJava(writer, "_result.get(" + Conversions.getValueLayout(returnValue.type) + ", 0)", false);
                writer.write(";\n");
            }
            writer.decreaseIndent();
            writer.write("} catch (Throwable _err) {\n");
            writer.write("    throw new AssertionError(\"Unexpected exception occured: \", _err);\n");
            writer.write("}\n");
            writer.decreaseIndent();
            writer.write("}\n");
        }
    }
}
