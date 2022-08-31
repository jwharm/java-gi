package girparser.model;

import java.io.IOException;
import java.io.Writer;

public class Callback extends RegisteredType implements CallableType {

    public ReturnValue returnValue;
    public Parameters parameters;

    public Callback(GirElement parent, String name) {
        super(parent, name, null);
    }

    public void generate(Writer writer) throws IOException {

        /////////////////////////////////////////////
        // TODO: Can we support arrays, varargs and out parameters?
        if (parameters != null && parameters.parameterList.stream().anyMatch(p -> p.type == null)) return;
        if (returnValue.type == null) return;
        /////////////////////////////////////////////

        generatePackageDeclaration(writer);

        writer.write("@FunctionalInterface\n");
        writer.write("public interface " + javaName + " {\n");
        writer.write("\n");

        writeMethodDeclaration(writer, doc, "on" + javaName, null, false);
        writer.write(";\n");
        writer.write("}\n");
    }

    // TODO: placeholder
    public void generateInterop(Writer writer, String identifier) throws IOException {
//        var params = parameters.parameterList;
//        if (params.size() == 0 && returnValue.type.equals("none") {
//            writer.write("Upcalls.createUpcallStub_Void(")
//        }

//        writer.write(identifier + ".generateUpcallStub()");

        writer.write("null");
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
