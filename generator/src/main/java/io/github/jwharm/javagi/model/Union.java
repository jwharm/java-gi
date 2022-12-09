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

        writer.write("public class " + javaName + " extends ObjectBase {\n");
        generateEnsureInitialized(writer);
        generateCType(writer);
        generateMemoryLayout(writer);
        generateMemoryAddressConstructor(writer);
        generateMarshal(writer);
        writer.write("}\n");
        writer.write("\n");
    }
}
