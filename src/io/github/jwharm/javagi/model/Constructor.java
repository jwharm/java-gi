package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.Writer;

public class Constructor extends Method {

    public Constructor(GirElement parent, String name, String cIdentifier, String deprecated, String throws_) {
        super(parent, name, cIdentifier, deprecated, throws_);
    }

    public void generate(Writer writer) throws IOException {

        // Do not generate deprecated constructors.
        if ("1".equals(deprecated)) {
            return;
        }

        if (doc != null) {
            doc.generate(writer, 1);
        }
        writer.write("    public ");
        writer.write(((RegisteredType) parent).javaName);
        if (parameters != null) {
            writer.write("(");
            parameters.generateJavaParameters(writer);
            writer.write(")");
        } else {
            writer.write("()");
        }
        writer.write(" {\n");
        writer.write("        super(ProxyFactory.getProxy(io.github.jwharm.javagi.interop.jextract.gtk_h." + cIdentifier);
        if (parameters != null) {
            writer.write("(");
            parameters.generateCParameters(writer, throws_);
            writer.write(")");
        } else {
            writer.write("()");
        }
        writer.write("));\n");
        writer.write("    }\n");
        writer.write("    \n");
    }

    public void generateNamed(Writer writer) throws IOException {
        RegisteredType clazz = (RegisteredType) parent;

        if (doc != null) {
            doc.generate(writer, 1);
        }
        writer.write("    public static " + clazz.javaName + " " + Conversions.toLowerCaseJavaName(name));
        if (parameters != null) {
            writer.write("(");
            parameters.generateJavaParameters(writer);
            writer.write(")");
        } else {
            writer.write("()");
        }
        writer.write(" {\n");
        writer.write("        return new " + clazz.javaName + "(ProxyFactory.getProxy(io.github.jwharm.javagi.interop.jextract.gtk_h." + cIdentifier);
        if (parameters != null) {
            writer.write("(");
            parameters.generateCParameters(writer, throws_);
            writer.write(")");
        } else {
            writer.write("()");
        }
        writer.write("));\n");
        writer.write("    }\n");
        writer.write("    \n");
    }
}
