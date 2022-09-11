package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.BindingsGenerator;
import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class Callback extends RegisteredType implements CallableType {

    public ReturnValue returnValue;
    public Parameters parameters;

    public Callback(GirElement parent, String name) {
        super(parent, name, null);
    }

    public void generate(Writer writer) throws IOException {

        if (returnValue.type == null) return;

        generateFunctionalInterface(writer);
        generateStaticCallback();
    }

    private void generateFunctionalInterface(Writer writer) throws IOException {
        generatePackageDeclaration(writer);

        writer.write("@FunctionalInterface\n");
        writer.write("public interface " + javaName + " {\n");
        writer.write("\n");
        writer.write("        void on" + javaName + "(");

        if (parameters != null) {
            // Write all parameters except the final userdata pointer
            for (int p = 0; p < parameters.parameterList.size() - 1; p++) {
                Parameter parameter = parameters.parameterList.get(p);
                parameter.generateTypeAndName(writer);
                if (p < parameters.parameterList.size() - 2) {
                    writer.write(", ");
                }
            }
        }
        writer.write(");\n");
        writer.write("}\n");
    }


    // Generate the static callback method, that will run the handler method.
    private void generateStaticCallback() throws IOException {
        StringWriter sw = new StringWriter();

        sw.write("    public static void " + "cb" + javaName + "(");

        String dataParamName = "";
        if (parameters != null) {
            for (int p = 0; p < parameters.parameterList.size(); p++) {
                if (p > 0) {
                    sw.write(", ");
                }
                Parameter parameter = parameters.parameterList.get(p);
                sw.write (Conversions.toPanamaJavaType(parameter.type) + " ");
                dataParamName = Conversions.toLowerCaseJavaName(parameter.name);
                sw.write(dataParamName);
            }
        }
        sw.write(") {\n");

        sw.write("        int hash = " + dataParamName + ".get(C_INT, 0);\n");
        sw.write("        var handler = (" + javaName + ") signalRegistry.get(hash);\n");
        sw.write("        handler.on" + javaName + "(");

        if (parameters != null) {
            for (int p = 0; p < parameters.parameterList.size() - 1; p++) {
                if (p != 0) {
                    sw.write(", ");
                }
                parameters.parameterList.get(p).generateCallbackInterop(sw);
            }
        }
        sw.write(");\n");

        sw.write("    }\n");
        sw.write("    \n");
        BindingsGenerator.signalCallbackFunctions.append(sw);
    }

    @Override
    public Parameters getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Parameters ps) {
        this.parameters = ps;
    }

    @Override
    public ReturnValue getReturnValue() {
        return returnValue;
    }

    @Override
    public void setReturnValue(ReturnValue rv) {
        this.returnValue = rv;
    }
}
