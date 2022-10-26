package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

public class Interface extends RegisteredType {

    public Prerequisite prerequisite;

    public Interface(GirElement parent, String name, String cType, String version) {
        super(parent, name, null, cType, version);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public interface " + javaName + " extends io.github.jwharm.javagi.Proxy {\n");

        for (Method m : methodList) {
            m.generate(writer, true, false);
        }

        for (Function function : functionList) {
            function.generate(writer, true, true);
        }

        for (Signal s : signalList) {
            s.generate(writer, true);
        }
        
        generateDowncallHandles(writer, true);
        generateSignalCallbacks(writer, true);
        generateImplClass(writer);

        writer.write("}\n");
    }

    public void generateImplClass(Writer writer) throws IOException {
    	writer.write("    \n");
        writer.write("    class " + javaName + "Impl extends org.gtk.gobject.Object implements " + javaName + " {\n");
        generateEnsureInitialized(writer, "        ");
    	writer.write("        \n");
        writer.write("        public " + javaName + "Impl(io.github.jwharm.javagi.Refcounted ref) {\n");
        writer.write("            super(ref);\n");
        writer.write("        }\n");
        writer.write("    }\n");
    }

    public String getInteropString(String paramName, boolean isPointer, boolean transferOwnership) {
        if (transferOwnership) {
            return paramName + ".refcounted().unowned().handle()";
        } else {
            return paramName + ".handle()";
        }
    }
}
