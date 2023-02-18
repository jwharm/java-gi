package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.GObjectBuilder;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;
import java.util.Objects;

public class Class extends RegisteredType {
    
    public String typeName;
    public String getType;
    public String typeStruct;
    public String abstract_;
    public String final_;
    
    public Record classStruct;

    public Class(GirElement parent, String name, String parentClass, String cType, String typeName, String getType,
            String typeStruct, String version, String abstract_, String final_) {
        
        super(parent, name, parentClass, cType, version);
        this.typeName = typeName;
        this.getType = getType;
        this.typeStruct = typeStruct;
        this.abstract_ = abstract_;
        this.final_ = final_;

        // Generate a function declaration to retrieve the type of this object.
        if (! (this instanceof Record)) {
            if (! "intern".equals(getType)) {
                registerGetTypeFunction(getType);
            }
        }
    }

    public void generate(SourceWriter writer) throws IOException {
        classStruct = (Record) Conversions.cTypeLookupTable.get(getNamespace().cIdentifierPrefix + typeStruct);
        
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public ");

        // Abstract classes
        if ("1".equals(abstract_)) {
            writer.write("abstract ");
        }

        // Final class classes
        if ("1".equals(final_)) {
            writer.write("final ");
        }

        writer.write("class " + javaName);

        // Generic types
        if (generic) {
            writer.write("<T extends org.gnome.gobject.GObject>");
        }

        // Parent class
        writer.write(" extends ");
        writer.write(Objects.requireNonNullElse(parentClass, "io.github.jwharm.javagi.base.ObjectProxy"));

        // Interfaces
        for (int i = 0; i < implementsList.size(); i++) {
            if (i == 0) {
                writer.write(" implements " + implementsList.get(i).getQualifiedJavaName());
            } else {
                writer.write(", " + implementsList.get(i).getQualifiedJavaName());
            }
        }

        // AutoCloseable
        if (autoCloseable) {
            if (implementsList.isEmpty()) {
                writer.write(" implements io.github.jwharm.javagi.util.AutoCloseable");
            } else {
                writer.write(", io.github.jwharm.javagi.util.AutoCloseable");
            }
        }

        writer.write(" {\n");
        writer.increaseIndent();

        generateMemoryAddressConstructor(writer);
        generateEnsureInitialized(writer);
        generateMemoryLayout(writer);
        generateConstructors(writer);
        generateMethodsAndSignals(writer);

        if (classStruct != null) {
            classStruct.generate(writer);
        }
        
        if (isInstanceOf("org.gnome.gobject.GObject")) {
            GObjectBuilder.generateBuilder(writer, this);
        }
        generateDowncallHandles(writer);

        // Generate a custom getType() function for ParamSpec
        if (isInstanceOf("org.gnome.gobject.ParamSpec") && "intern".equals(getType)) {
            writer.write("\n");
            writer.write("public static org.gnome.glib.Type getType() {\n");
            writer.write("    return org.gnome.glib.Type.G_TYPE_PARAM;\n");
            writer.write("}\n");
            writer.write("\n");
            writer.write("public static boolean isAvailable() {\n");
            writer.write("    return true;\n");
            writer.write("}\n");
        } else {
            generateIsAvailable(writer);
        }

        // Abstract classes
        if ("1".equals(abstract_)) {
            generateImplClass(writer);
        }

        generateInjected(writer);

        writer.decreaseIndent();
        writer.write("}\n");
    }

    public String getConstructorString() {
        String qName = Conversions.convertToJavaType(this.javaName, true, getNamespace());
        return ("1".equals(abstract_)
                ? qName + "." + this.javaName + "Impl::new"
                : qName + "::new");
    }
}
