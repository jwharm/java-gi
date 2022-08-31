package girparser.model;

import java.io.IOException;
import java.io.Writer;

public class Method extends GirElement implements CallableType {

    public final String cIdentifier, deprecated, throws_;
    public ReturnValue returnValue;
    public Parameters parameters;

    public Method(GirElement parent, String name, String cIdentifier, String deprecated, String throws_) {
        super(parent);
        this.name = name;
        this.cIdentifier = cIdentifier;
        this.deprecated = deprecated;
        this.throws_ = throws_;

        // Handle empty names. (For example, GLib.g_iconv is named "".)
        if ("".equals(name)) {
            this.name = cIdentifier;
        }
    }

    public void generate(Writer writer, boolean isDefault, boolean isStatic) throws IOException {

        // Do not generate deprecated methods.
        if ("1".equals(deprecated)) {
            return;
        }

        writeMethodDeclaration(writer, doc, name, throws_, isDefault, isStatic);
        writer.write(" {\n");

        if (throws_ != null) {
            writer.write("        MemorySegment GERROR = org.gtk.interop.jextract.GError.allocate(Interop.getScope());\n");
        }
        writer.write("        ");
        if (! returnValue.type.isVoid()) {
            writer.write("var RESULT = ");
        }
        writer.write("org.gtk.interop.jextract.gtk_h." + cIdentifier);
        if (parameters != null) {
            writer.write("(");
            parameters.generateCParameters(writer, throws_);
            writer.write(")");
        } else {
            writer.write("()");
        }
        writer.write(";\n");

        if (throws_ != null) {
            writer.write("        if (! java.util.Objects.equals(MemoryAddress.NULL, GERROR)) {\n");
            writer.write("            throw new org.gtk.interop.GErrorException(GERROR);\n");
            writer.write("        }\n");
        }
        returnValue.generateReturnStatement(writer, 2);

        writer.write("    }\n");
        writer.write("    \n");
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
