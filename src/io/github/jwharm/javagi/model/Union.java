package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

public class Union extends RegisteredType {

    public Union(GirElement parent, String name, String cType) {
        super(parent, name, null, cType);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);

        writer.write("import io.github.jwharm.javagi.Proxy;\n");
        writer.write("import java.lang.foreign.MemoryAddress;\n");
        writer.write("\n");

        generateJavadoc(writer);

        writer.write("public class " + javaName + " extends io.github.jwharm.javagi.ResourceBase {\n");
        writer.write("    \n");
        generateMemoryAddressConstructor(writer);
        writer.write("}\n");
        writer.write("\n");
    }

    public String getInteropString(String paramName, boolean isPointer, boolean transferOwnership) {
        if (transferOwnership) {
            return paramName + ".getReference().unowned().handle()";
        } else {
            return paramName + ".handle()";
        }
    }
}
