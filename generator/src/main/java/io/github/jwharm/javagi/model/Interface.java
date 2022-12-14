package io.github.jwharm.javagi.model;

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

        writer.write("public interface " + javaName + " extends io.github.jwharm.javagi.Proxy {\n");
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
        
        generateDowncallHandles(writer);
        generateImplClass(writer);

        generateInjected(writer);

        generateIsAvailable(writer);

        writer.decreaseIndent();
        writer.write("}\n");
    }

    public void generateImplClass(SourceWriter writer) throws IOException {
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * The " + javaName + "Impl type represents a native instance of the " + javaName + " interface.\n");
        writer.write(" */\n");
        writer.write("class " + javaName + "Impl extends org.gtk.gobject.GObject implements " + javaName + " {\n");
        writer.increaseIndent();

        generateEnsureInitialized(writer);

        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Creates a new instance of " + javaName + " for the provided memory address.\n");
        writer.write(" * @param address the memory address of the instance\n");
        writer.write(" */\n");
        writer.write("public " + javaName + "Impl(Addressable address) {\n");
        writer.write("    super(address);\n");
        writer.write("}\n");

        writer.decreaseIndent();
        writer.write("}\n");
    }
}
