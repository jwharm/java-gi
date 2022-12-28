package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class Parameters extends GirElement {

    public final List<Parameter> parameterList = new ArrayList<>();

    public Parameters(GirElement parent) {
        super(parent);
    }

    public Parameter getCallbackParameter() {
        for (Parameter p : parameterList) {
            if (p.isCallbackParameter() && (! p.isDestroyNotify()))
                return p;
        }
        return null;
    }

    public boolean hasCallbackParameter() {
        return getCallbackParameter() != null;
    }
    
    public void generateJavaParameters(Writer writer, boolean pointerForArray) throws IOException {
        int counter = 0;
        for (Parameter p : parameterList) {
            if (p.isInstanceParameter()) {
                continue;
            }
            if (hasCallbackParameter() && (p.isUserDataParameter() || p.isDestroyNotify())) {
                continue;
            }
            if (counter++ > 0) {
                writer.write(", ");
            }
            p.generateTypeAndName(writer, pointerForArray);
        }
    }

    public void generateJavaParameterNames(Writer writer) throws IOException {
        int counter = 0;
        for (Parameter p : parameterList) {
            if (p.isInstanceParameter()) {
                continue;
            }
            if (hasCallbackParameter() && (p.isUserDataParameter() || p.isDestroyNotify())) {
                continue;
            }
            if (counter++ > 0) {
                writer.write(", ");
            }
            writer.write(p.varargs ? "varargs" : p.name);
        }
    }

    public void generateCParameters(Writer writer, String throws_) throws IOException {
        int counter = 0;

        Parameter callbackParameter = getCallbackParameter();
        Callback callback = null;
        String callbackParamName = null;
        if (callbackParameter != null) {
            callback = (Callback) callbackParameter.type.girElementInstance;
            callbackParamName = callbackParameter.name;
        }

        for (Parameter p : parameterList) {
            if (counter++ > 0) {
                writer.write(",");
            }
            writer.write("\n                    ");
            // Don't null-check parameters that are hidden from the Java API, or primitive values
            if (p.checkNull()) {
                writer.write("(Addressable) (" + (p.varargs ? "varargs" : p.name) + " == null ? MemoryAddress.NULL : ");
            }
            if (p.isInstanceParameter()) {
                writer.write("handle()");
            } else if (p.isDestroyNotify()) {
                writer.write("Interop.cbDestroyNotifySymbol()");
            } else if (p.isCallbackParameter()) {
                String className = p.type.getNamespace().globalClassName;
                writer.write("(Addressable) Linker.nativeLinker().upcallStub(\n");
                writer.write("                        MethodHandles.lookup().findStatic(" + className + ".Callbacks.class, \"cb" + p.type.simpleJavaType + "\",\n");
                writer.write("                            MethodType.methodType(");
                writer.write(Conversions.toPanamaJavaType(callback.returnValue.type) + ".class");
                if (callback.parameters != null) {
                    for (Parameter cbp : callback.parameters.parameterList) {
                        writer.write(", " + Conversions.toPanamaJavaType(cbp.type) + ".class");
                    }
                }
                writer.write(")),\n");
                writer.write("                        FunctionDescriptor.");
                if (callback.returnValue.type == null || "void".equals(callback.returnValue.type.simpleJavaType)) {
                    writer.write("ofVoid(");
                } else {
                    writer.write("of(" + Conversions.toPanamaMemoryLayout(callback.returnValue.type));
                    if (callback.parameters != null) {
                        writer.write(", ");
                    }
                }
                if (callback.parameters != null) {
                    for (int i = 0; i < callback.parameters.parameterList.size(); i++) {
                        if (i > 0) {
                            writer.write(", ");
                        }
                        writer.write(Conversions.toPanamaMemoryLayout(callback.parameters.parameterList.get(i).type));
                    }
                }
                writer.write("),\n");
                writer.write("                        Interop.getScope())");
            } else if (callback != null && p.isUserDataParameter()) {
                writer.write("(Addressable) (");
                if (callbackParameter.nullable) {
                    writer.write(callbackParamName + " == null ? MemoryAddress.NULL : ");
                }
                writer.write("Interop.registerCallback(" + callbackParamName + "))");
            } else if (p.varargs) {
                writer.write("varargs");
            } else {
                p.generateInterop(writer, p.name, true);
            }
            if (p.checkNull()) {
                writer.write(")");
            }
        }
        if (throws_ != null) {
            writer.write(",\n                    (Addressable) GERROR");
        }
    }

    /**
     * Generate preprocessing statements for all parameters
     * @param writer The source code file writer
     * @param indent How many tabs to indent
     * @throws IOException Thrown when an error occurs while writing
     */
    public void generatePreprocessing(Writer writer, int indent) throws IOException {
        for (Parameter p : parameterList) {
            p.generatePreprocessing(writer, indent);
        }
    }
    
    /**
     * Generate postprocessing statements for all parameters
     * @param writer The source code file writer
     * @param indent How many tabs to indent
     * @throws IOException Thrown when an error occurs while writing
     */
    public void generatePostprocessing(Writer writer, int indent) throws IOException {
        // First the regular (non-array) out-parameters. These could include an out-parameter with 
        // the length of an array out-parameter, so we have to process these first.
        for (Parameter p : parameterList) {
            if (p.array == null) {
                p.generatePostprocessing(writer, indent);
            }
        }
        // Secondly, process the array out parameters
        for (Parameter p : parameterList) {
            if (p.array != null) {
                p.generatePostprocessing(writer, indent);
            }
        }
    }
}
