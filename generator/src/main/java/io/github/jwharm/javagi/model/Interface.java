package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.GObjectBuilder;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;

public class Interface extends RegisteredType {

    public String typeName;
    public String getType;
    public String typeStruct;
    public Prerequisite prerequisite;

    public Record classStruct;
    
    public Interface(GirElement parent, String name, String cType, String typeName, String getType,
            String typeStruct, String version) {
        
        super(parent, name, null, cType, version);
        this.typeName = typeName;
        this.getType = getType;
        this.typeStruct = typeStruct;
        
        // Generate a function declaration to retrieve the type of this object.
        registerGetTypeFunction(getType);
    }

    public void generate(SourceWriter writer) throws IOException {
        classStruct = (Record) Conversions.cTypeLookupTable.get(getNamespace().cIdentifierPrefix + typeStruct);
        
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public interface " + javaName);
        if (generic) {
            writer.write("<T extends org.gnome.gobject.GObject>");
        }
        writer.write(" extends io.github.jwharm.javagi.base.Proxy {\n");
        writer.increaseIndent();

        generateMarshal(writer);
        generateMethodsAndSignals(writer);

        if (classStruct != null) {
            classStruct.generate(writer);
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
