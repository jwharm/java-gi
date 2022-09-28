package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

public class Interface extends RegisteredType {

    public Prerequisite prerequisite;

    public Interface(GirElement parent, String name, String cType) {
        super(parent, name, null, cType);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public interface " + javaName + " extends io.github.jwharm.javagi.Proxy {\n");
        writer.write("\n");

        for (Method m : methodList) {
            if (m.isSafeToBind()) {
                m.generate(writer, true, false);
            }
        }

        for (Function function : functionList) {
            if (function.isSafeToBind()) {
                function.generate(writer, false, true);
            }
        }

        for (Signal s : signalList) {
            if (s.isSafeToBind()) {
                s.generate(writer, true);
            }
        }

        generateImplClass(writer);

        writer.write("}\n");
    }

    public void generateImplClass(Writer writer) throws IOException {
        writer.write("    class " + javaName + "Impl extends org.gtk.gobject.Object implements " + javaName + " {\n");
        writer.write("        public " + javaName + "Impl(io.github.jwharm.javagi.Reference reference) {\n");
        writer.write("            super(reference);\n");
        writer.write("        }\n");
        writer.write("    }\n");
    }

    public String getInteropString(String paramName, boolean isPointer, boolean transferOwnership) {
        if (transferOwnership) {
            return paramName + ".getReference().unowned().handle()";
        } else {
            return paramName + ".handle()";
        }
    }
}
