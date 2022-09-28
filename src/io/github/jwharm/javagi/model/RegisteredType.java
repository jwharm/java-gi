package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

import java.io.IOException;
import java.io.Writer;

public abstract class RegisteredType extends GirElement {

    public final String javaName, parentClass, cType;

    public RegisteredType(GirElement parent, String name, String parentClass, String cType) {
        super(parent);
        this.parentClass = Conversions.toQualifiedJavaType(parentClass);
        this.name = name;
        this.cType = cType;
        this.javaName = Conversions.toSimpleJavaType(name);
    }

    public abstract void generate(Writer writer) throws IOException;

    protected void generatePackageDeclaration(Writer writer) throws IOException {
        writer.write("package " + getNamespace().packageName + ";\n");
        writer.write("\n");
    }

    public static void generateImportStatements(Writer writer) throws IOException {
        writer.write("import io.github.jwharm.javagi.interop.jextract.gtk_h;\n");
        writer.write("import io.github.jwharm.javagi.*;\n");
        writer.write("import java.lang.foreign.*;\n");
        writer.write("import java.lang.invoke.*;\n");
        writer.write("\n");
    }

    protected void generateJavadoc(Writer writer) throws IOException {
        if (doc != null) {
            doc.generate(writer, 0);
        }
    }

    /**
     * Generate standard constructors from a MemoryAddress and a GObject
     */
    protected void generateCastFromGObject(Writer writer) throws IOException {
        writer.write("    /** Cast object to " + javaName + " */\n");
        writer.write("    public static " + javaName + " castFrom(org.gtk.gobject.Object gobject) {\n");
        writer.write("        return new " + javaName + "(gobject.getReference());\n");
        writer.write("    }\n");
        writer.write("    \n");
    }

    protected void generateMemoryAddressConstructor(Writer writer) throws IOException {
        writer.write("    public " + javaName + "(io.github.jwharm.javagi.Reference reference) {\n");
        writer.write("        super(reference);\n");
        writer.write("    }\n");
        writer.write("    \n");
    }

    /**
     * Generates all constructors listed for this type. When the constructor is not named "new", a static
     * factory method is generated with the provided name.
     */
    protected void generateConstructors(Writer writer) throws IOException {
        for (Constructor c : constructorList) {
            if (c.isSafeToBind()) {
                if (c.name.equals("new")) {
                    c.generate(writer);
                } else {
                    c.generateNamed(writer);
                }
            }
        }
    }
    
    public abstract String getInteropString(String paramName, boolean isPointer, boolean transferOwnership);
}
