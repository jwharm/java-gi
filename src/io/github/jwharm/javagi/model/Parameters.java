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

    public void generateJavaParameters(Writer writer) throws IOException {
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
            p.generateTypeAndName(writer);
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
            writer.write(p.name);
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
                writer.write(", ");
            }
            if (p.isInstanceParameter()) {
                writer.write("handle()");
            } else if (p.isDestroyNotify()) {
                writer.write("\n");
                writer.write("                    Interop.cbDestroyNotifySymbol()");
            } else if (p.isCallbackParameter()) {
                String className = Conversions.toSimpleJavaType(p.type.getNamespace().name);
                writer.write("\n");
                writer.write("                    Linker.nativeLinker().upcallStub(\n");
                writer.write("                        MethodHandles.lookup().findStatic(" + className + ".class, \"__cb" + p.type.simpleJavaType + "\",\n");
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
                writer.write("\n");
                writer.write("                    Interop.getAllocator().allocate(C_INT, Interop.registerCallback("
                        + callbackParamName + ".hashCode(), " + callbackParamName + "))");
            } else {
                p.generateInterop(writer);
            }
        }
        if (throws_ != null) {
            writer.write(", GERROR");
        }
    }
}
