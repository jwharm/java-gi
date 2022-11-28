package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.BindingsGenerator;
import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class Callback extends RegisteredType implements CallableType {

    public ReturnValue returnValue;
    public Parameters parameters;

    public Callback(GirElement parent, String name, String cType, String version) {
        super(parent, name, null, cType, version);
    }

    public void generate(Writer writer) throws IOException {
        generateFunctionalInterface(writer);
        generateStaticCallback();
    }

    private void generateFunctionalInterface(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);
        
        writer.write("@FunctionalInterface\n");
        writer.write("public interface " + javaName + " {\n");
        writer.write("        ");
        if (returnValue.type == null) {
            writer.write("void");
        } else {
            writer.write(returnValue.type.qualifiedJavaType);
        }
        writer.write(" on" + javaName + "(");
        if (parameters != null) {
            int counter = 0;
            for (Parameter p : parameters.parameterList) {
                if (! (p.isUserDataParameter() || p.isDestroyNotify())) {
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

        writer.write("        \n");
        writer.write("        public static ");
        if (returnValue.type == null) {
            writer.write("void");
        } else {
            // Pointer parameters are "MemoryAddress", but return values are "Addressable"
            String returnType = Conversions.toPanamaJavaType(returnValue.type);
            if (returnType.equals("MemoryAddress")) {
                returnType = "Addressable";
            }
            writer.write(returnType);
        }
        writer.write(" cb" + javaName + "(");

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

        writer.write("            int HASH = " + dataParamName + ".get(Interop.valueLayout.C_INT, 0);\n");
        writer.write("            var HANDLER = (" + javaName + ") Interop.signalRegistry.get(HASH);\n");
        
        // For out-parameters, create a local Out<> object and pass that to the callback.
        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
                if (p.isOutParameter()) {
                    writer.write("        var " + p.name + "OUT = new Out<" + p.getReturnType() + ">(");
                    p.generateReverseInterop(writer, p.name, true);
                    writer.write(");\n");
                }
            }
        }
        
        writer.write("            ");
        if ((returnValue.type != null) && (! "void".equals(returnValue.type.simpleJavaType))) {
            writer.write("var RESULT = ");
        }
        writer.write("HANDLER.on" + javaName + "(");

        if (parameters != null) {
            int counter = 0;
            for (Parameter p : parameters.parameterList) {
                if (! (p.isUserDataParameter() || p.isDestroyNotify())) {
                    if (counter > 0) {
                        writer.write(", ");
                    }
                    if (p.isOutParameter()) {
                        writer.write(p.name + "OUT");
                    } else {
                        p.generateReverseInterop(writer, p.name, true);
                    }
                    counter++;
                }
            }
        }
        writer.write(");\n");
        
        // For out-parameters, read the value of the Out<> object that was generated above, 
        // and write the value to the out-parameter memory address that was passed from the native code.
        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
                if (p.isOutParameter()) {
                    writer.write("            " + p.name + ".set(" + Conversions.getValueLayout(p.type) + ", 0, ");
                    p.generateInterop(writer, p.name + "OUT.get()", false);
                    writer.write(");\n");
                }
            }
        }

        if ((returnValue.type != null) && (! "void".equals(returnValue.type.simpleJavaType))) {
            writer.write("            return ");
            returnValue.generateInterop(writer, "RESULT", false);
            writer.write(";\n");
        }
        writer.write("        }\n");
        BindingsGenerator.signalCallbackFunctions.append(writer);
    }
    
    @Override
    public String getInteropString(String paramName, boolean isPointer, String transferOwnership) {
        return paramName; // TODO
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
