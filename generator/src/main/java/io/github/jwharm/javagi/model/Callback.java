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
        writer.write(" run(");
        if (parameters != null) {
            int counter = 0;
            for (Parameter p : parameters.parameterList) {
                if (counter > 0) {
                    writer.write(", ");
                }
                p.generateTypeAndName(writer, true);
                counter++;
            }
        }
        writer.write(");\n");
        writer.write("}\n");
    }

    @Override
    public String getInteropString(String paramName, boolean isPointer, String transferOwnership) {
        String methodType = "MethodType.methodType(";
        methodType += Conversions.toPanamaJavaType(returnValue.type) + ".class";
        if (parameters != null) {
            for (Parameter cbp : parameters.parameterList) {
                methodType += ", " + Conversions.toPanamaJavaType(cbp.type) + ".class";
            }
        }
        methodType += ")";

        String functionDescriptor = "FunctionDescriptor.";
        if (returnValue.type == null || "void".equals(returnValue.type.simpleJavaType)) {
            functionDescriptor += "ofVoid(";
        } else {
            functionDescriptor += "of(" + Conversions.toPanamaMemoryLayout(returnValue.type);
            if (parameters != null) {
                functionDescriptor += ", ";
            }
        }
        if (parameters != null) {
            for (int i = 0; i < parameters.parameterList.size(); i++) {
                if (i > 0) {
                    functionDescriptor += ", ";
                }
                functionDescriptor += Conversions.toPanamaMemoryLayout(parameters.parameterList.get(i).type);
            }
        }
        functionDescriptor += ")";

        String marshals = "new Marshal[] {";
        marshals += Conversions.getMarshal(returnValue.type);
        if (parameters != null) {
            for (Parameter cbp : parameters.parameterList) {
                marshals += ", " + Conversions.getMarshal(cbp.type);
            }
        }
        marshals += "}";

        String indent = " ".repeat(24);

        return "Interop.toCallback(\n" +
                indent + paramName + ",\n" +
                indent + methodType + ",\n" +
                indent + functionDescriptor +",\n" +
                indent + marshals + ")";
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
