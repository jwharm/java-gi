package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.BindingsGenerator;
import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class Callback extends RegisteredType implements CallableType {

    public ReturnValue returnValue;
    public Parameters parameters;

    public Callback(GirElement parent, String name, String cType) {
        super(parent, name, null, cType);
    }

    public void generate(Writer writer) throws IOException {
        generateFunctionalInterface(writer);
        generateStaticCallback();
    }

    private void generateFunctionalInterface(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        writer.write("import io.github.jwharm.javagi.*;\n");
        writer.write("\n");
        generateJavadoc(writer);
        writer.write("@FunctionalInterface\n");
        writer.write("public interface " + javaName + " {\n");
        writer.write("        ");
        if (returnValue.type == null) {
            writer.write("void");
        } else if (returnValue.type.isBitfield()) {
            writer.write("int");
        } else {
            writer.write(returnValue.type.qualifiedJavaType);
        }
        writer.write(" on" + javaName + "(");
        if (parameters != null) {
            int counter = 0;
            for (Parameter p : parameters.parameterList) {
                // Exclude GError** parameters for now
                if (! (p.isUserDataParameter() || p.isDestroyNotify() || p.isErrorParameter())) {
                    if (counter > 0) {
                        writer.write(", ");
                    }
                    p.generateTypeAndName(writer, true);
                    counter++;
                }
            }
        }
        writer.write(");\n");
        writer.write("}\n");
    }

    // Generate the static callback method, that will run the handler method.
    private void generateStaticCallback() throws IOException {
        StringWriter writer = new StringWriter();

        writer.write("    public static ");
        if (returnValue.type == null) {
            writer.write("void");
        } else if (returnValue.type.isBitfield()) {
            writer.write("int");
        } else {
            writer.write(returnValue.type.qualifiedJavaType);
        }
        writer.write(" __cb" + javaName + "(");

        String dataParamName = "";
        if (parameters != null) {
            int counter = 0;
            for (Parameter p : parameters.parameterList) {
                if (counter > 0) {
                    writer.write(", ");
                }
                writer.write (Conversions.toPanamaJavaType(p.type) + " ");
                writer.write(Conversions.toLowerCaseJavaName(p.name));
                if (p.isUserDataParameter()) {
                    dataParamName = Conversions.toLowerCaseJavaName(p.name);
                }
                counter++;
            }
        }
        writer.write(") {\n");

        // Cannot handle callback without user_data parameter.
        if (dataParamName.equals("")) {
            return;
        }

        writer.write("        int hash = " + dataParamName + ".get(ValueLayout.JAVA_INT, 0);\n");
        writer.write("        var handler = (" + javaName + ") Interop.signalRegistry.get(hash);\n");
        writer.write("        ");
        if ((returnValue.type != null) && (! "void".equals(returnValue.type.simpleJavaType))) {
            writer.write("return ");
        }
        writer.write("handler.on" + javaName + "(");

        if (parameters != null) {
            int counter = 0;
            for (Parameter p : parameters.parameterList) {
                // Exclude GError** parameters for now
                if (p.isUserDataParameter() || p.isDestroyNotify() || p.isErrorParameter()) {
                    continue;
                }
                if (counter > 0) {
                    writer.write(", ");
                }
                p.generateReverseInterop(writer, p.name);
                counter++;
            }
        }
        writer.write(");\n");

        writer.write("    }\n");
        writer.write("    \n");
        BindingsGenerator.signalCallbackFunctions.append(writer);
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

    public String getInteropString(String paramName, boolean isPointer, boolean transferOwnership) {
        if (transferOwnership) {
            return paramName + ".refcounted().unowned().handle()";
        } else {
            return paramName + ".handle()";
        }
    }
}
