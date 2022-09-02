package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

public class Interface extends RegisteredType {

    public Prerequisite prerequisite;

    public Interface(GirElement parent, String name) {
        super(parent, name, null);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public interface " + javaName + " extends io.github.jwharm.javagi.interop.NativeAddress {\n");
        writer.write("\n");

        for (Method m : methodList) {
            if (m.isSafeToBind()) {
                m.generate(writer, true, false);
            }
        }

        for (Signal s : signalList) {
            if (s.isSafeToBind()) {
                s.generate(writer, true);
            }
        }

        generateProxyInstance(writer);

        writer.write("}\n");
    }

    public void generateProxyInstance(Writer writer) throws IOException {
        writer.write("    class " + javaName + "ProxyInstance extends org.gtk.gobject.Object implements " + javaName + " {\n");
        writer.write("        public " + javaName + "ProxyInstance(io.github.jwharm.javagi.interop.Proxy proxy) {\n");
        writer.write("            super(proxy);\n");
        writer.write("        }\n");
        writer.write("    }\n");
    }
}
