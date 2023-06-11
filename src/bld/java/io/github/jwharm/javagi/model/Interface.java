package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.Builder;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;

public class Interface extends RegisteredType {

    public String typeName;
    public String typeStruct;
    public Prerequisite prerequisite;

    public Record classStruct;
    
    public Interface(GirElement parent, String name, String cType, String typeName, String getType,
            String typeStruct, String version) {
        
        super(parent, name, null, cType, getType, version);
        this.typeName = typeName;
        this.typeStruct = typeStruct;
    }

    public void generate(SourceWriter writer) throws IOException {
        classStruct = (Record) module().cTypeLookupTable.get(getNamespace().cIdentifierPrefix + typeStruct);
        
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public interface " + javaName);
        if (generic) {
            writer.write("<T extends org.gnome.gobject.GObject>");
        }
        writer.write(" extends io.github.jwharm.javagi.base.Proxy {\n");
        writer.increaseIndent();

        generateGType(writer);
        generateMethodsAndSignals(writer);

        if (classStruct != null) {
            classStruct.generate(writer);
        }
        
        Builder.generateInterfaceBuilder(writer, this);
        generateImplClass(writer);
        generateInjected(writer);

        writer.decreaseIndent();
        writer.write("}\n");
    }

    @Override
    public String getConstructorString() {
        String qName = Conversions.convertToJavaType(this.javaName, true, getNamespace());
        return qName + "." + this.javaName + "Impl::new";
    }
}
