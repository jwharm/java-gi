package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

public class Union extends RegisteredType {

    public Union(GirElement parent, String name, String cType, String version) {
        super(parent, name, null, cType, version);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public class " + javaName + " extends io.github.jwharm.javagi.ResourceBase {\n");
        generateEnsureInitialized(writer);
        generateCType(writer);
        generateMemoryLayout(writer);
        generateMemoryAddressConstructor(writer);
        writer.write("}\n");
        writer.write("\n");
    }

    public String getInteropString(String paramName, boolean isPointer, String transferOwnership) {
        if ("Ownership.FULL".equals(transferOwnership)) {
            return paramName + ".refcounted().unowned().handle()";
        } else {
            return paramName + ".handle()";
        }
    }
}
