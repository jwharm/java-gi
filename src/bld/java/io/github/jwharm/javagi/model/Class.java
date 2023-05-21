package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.Builder;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;
import java.util.Objects;
import java.util.StringJoiner;

public class Class extends RegisteredType {
    
    public String typeName;
    public String typeStruct;
    public String getValueFunc;
    public String setValueFunc;
    public String abstract_;
    public String final_;
    
    public Record classStruct;

    public Class(GirElement parent, String name, String parentClass, String cType, String typeName, String getType,
            String typeStruct, String getValueFunc, String setValueFunc, String version, String abstract_, String final_) {
        
        super(parent, name, parentClass, cType, getType, version);
        this.typeName = typeName;
        this.typeStruct = typeStruct;
        this.getValueFunc = getValueFunc;
        this.setValueFunc = setValueFunc;
        this.abstract_ = abstract_;
        this.final_ = final_;
    }

    public void generate(SourceWriter writer) throws IOException {
        classStruct = (Record) module().cTypeLookupTable.get(getNamespace().cIdentifierPrefix + typeStruct);
        
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public ");

        // Abstract classes
        if ("1".equals(abstract_)) {
            writer.write("abstract ");
        }

        // Final classes
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
        writer.write(Objects.requireNonNullElse(parentClass, "org.gnome.gobject.TypeInstance"));

        // Interfaces
        StringJoiner interfaces = new StringJoiner(", ", " implements ", "").setEmptyValue("");
        implementsList.forEach(impl -> interfaces.add(impl.getQualifiedJavaName()));
        if (autoCloseable) {
            interfaces.add("io.github.jwharm.javagi.util.AutoCloseable");
        }
        if (isFloating()) {
            interfaces.add("io.github.jwharm.javagi.base.Floating");
        }
        writer.write(interfaces + " {\n");
        writer.increaseIndent();

        generateMemoryAddressConstructor(writer);
        generateEnsureInitialized(writer);
        generateGType(writer);
        generateMemoryLayout(writer);
        generateConstructors(writer);
        generateMethodsAndSignals(writer);

        if (classStruct != null) {
            classStruct.generate(writer);
        }
        
        if (isInstanceOf("org.gnome.gobject.GObject")) {
            Builder.generateBuilder(writer, this);
        }

        // Generate a custom gtype declaration for ParamSpec
        if (isInstanceOf("org.gnome.gobject.ParamSpec") && "intern".equals(getType)) {
            writer.write("\n");
            writer.write("public static final org.gnome.glib.Type gtype = org.gnome.glib.Type.G_TYPE_PARAM;\n");
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
