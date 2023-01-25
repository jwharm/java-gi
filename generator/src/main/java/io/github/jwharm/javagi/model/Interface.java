package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.GObjectBuilder;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;

public class Interface extends RegisteredType {

    public String typeName, getType, typeStruct;
    public Prerequisite prerequisite;

    public Interface(GirElement parent, String name, String cType, String typeName, String getType,
            String typeStruct, String version) {
        
        super(parent, name, null, cType, version);
        
        // Generate a function declaration to retrieve the type of this object.
        registerGetTypeFunction(getType);
    }

    public void generate(SourceWriter writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public interface " + javaName + " extends io.github.jwharm.javagi.base.Proxy {\n");
        writer.increaseIndent();

        generateMarshal(writer);

        for (Method m : methodList) {
            m.generate(writer, true, false);
        }

        for (Function function : functionList) {
            function.generate(writer, true, true);
        }

        for (Signal s : signalList) {
            s.generate(writer, true);
        }

        GObjectBuilder.generateInterfaceBuilder(writer, this);
        generateDowncallHandles(writer);
        generateImplClass(writer);

        generateInjected(writer);

        generateIsAvailable(writer);

        writer.decreaseIndent();
        writer.write("}\n");
    }
}
