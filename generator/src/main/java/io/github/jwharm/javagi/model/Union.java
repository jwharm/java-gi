package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;

public class Union extends RegisteredType {

    public Union(GirElement parent, String name, String cType, String version) {
        super(parent, name, null, cType, version);
    }

    public void generate(SourceWriter writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public class " + javaName);
        if (generic)
            writer.write("<T extends org.gtk.gobject.GObject>");
        writer.write(" extends StructProxy {\n");
        writer.increaseIndent();
        generateEnsureInitialized(writer);
        generateCType(writer);
        generateMemoryLayout(writer);
        generateMemoryAddressConstructor(writer);
        generateMarshal(writer);
        generateInjected(writer);
        writer.decreaseIndent();
        writer.write("}\n");
    }
}
