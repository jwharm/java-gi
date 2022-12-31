package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.GObjectBuilder;
import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.Writer;

public class Class extends RegisteredType {
    
    public String typeName, getType, typeStruct, abstract_;

    public Class(GirElement parent, String name, String parentClass, String cType, String typeName, String getType,
            String typeStruct, String version, String abstract_) {
        
        super(parent, name, parentClass, cType, version);
        this.typeName = typeName;
        this.getType = getType;
        this.typeStruct = typeStruct;
        this.abstract_ = abstract_;

        // Generate a function declaration to retrieve the type of this object.
        if (! (this instanceof Record)) {
            if (! "intern".equals(getType)) {
                registerGetTypeFunction(getType);
            }
        }
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public ");

//        // Abstract classes - TODO: generate an Impl class to instantiate return values
//        if ("1".equals(abstract_)) {
//            writer.write("abstract ");
//        }

        writer.write("class " + javaName);

        // Parent class
        writer.write(" extends ");
        if (parentClass == null) {
            writer.write("io.github.jwharm.javagi.ObjectBase");
        } else {
            writer.write(parentClass);
        }

        // Interfaces
        for (int i = 0; i < implementsList.size(); i++) {
            if (i == 0) {
                writer.write(" implements " + Conversions.toQualifiedJavaType(implementsList.get(i).name, getNamespace()));
            } else {
                writer.write(", " + Conversions.toQualifiedJavaType(implementsList.get(i).name, getNamespace()));
            }
        }
        writer.write(" {\n");

        generateEnsureInitialized(writer);
        generateCType(writer);
        generateMemoryLayout(writer);
        generateMemoryAddressConstructor(writer);
        generateMarshal(writer);
        generateConstructors(writer);

        for (Method m : methodList) {
            m.generate(writer, false, false);
        }

        for (Function function : functionList) {
            function.generate(writer, false, true);
        }

        for (Signal s : signalList) {
            s.generate(writer, false);
        }

        if (isInstanceOf("org.gtk.gobject.GObject")) {
            GObjectBuilder.generateBuilder(writer, this);
        }
        generateDowncallHandles(writer);
        generateInjected(writer);

        // Generate a custom getType() function for ParamSpec
        if (isInstanceOf("org.gtk.gobject.ParamSpec") && "intern".equals(getType)) {
            writer.write("\n");
            writer.write("    public static org.gtk.glib.Type getType() {\n");
            writer.write("        return org.gtk.glib.Type.G_TYPE_PARAM;\n");
            writer.write("    }\n");
        }

        writer.write("}\n");
    }
}
