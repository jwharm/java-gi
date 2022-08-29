package girparser.model;

import girparser.generator.Conversions;

import java.io.IOException;
import java.io.Writer;

public abstract class RegisteredType extends GirElement {

    public final String javaName;

    public boolean used = false;

    public RegisteredType(GirElement parent, String name) {
        super(parent);
        this.name = name;
        this.javaName = Conversions.toSimpleJavaType(name);
    }

    public abstract void generate(Writer writer) throws IOException;

    protected void generatePackageDeclaration(Writer writer) throws IOException {
        writer.write("package " + getNamespace().packageName + ";\n");
        writer.write("\n");
    }

    protected void generateImportStatements(Writer writer) throws IOException {
        writer.write("import org.gtk.gobject.*;\n");
        writer.write("import org.gtk.interop.*;\n");
        writer.write("import jdk.incubator.foreign.*;\n");
        writer.write("import java.lang.invoke.*;\n");
        writer.write("\n");
    }

    protected void generateJavadoc(Writer writer) throws IOException {
        if (doc != null) {
            doc.generate(writer, 0);
        }
    }

    // Generate standard constructors from a MemoryAddress and a GObject
    protected void generateCastFromGObject(Writer writer) throws IOException {
        writer.write("    /** Cast object to " + javaName + " */\n");
        writer.write("    public static " + javaName + " castFrom(org.gtk.gobject.Object gobject) {\n");
        writer.write("        return new " + javaName + "(gobject.HANDLE());\n");
        writer.write("    }\n");
        writer.write("    \n");
    }

    protected void generateMemoryAddressConstructor(Writer writer) throws IOException {
        writer.write("    public " + javaName + "(MemoryAddress handle) {\n");
        writer.write("        super(handle);\n");
        writer.write("    }\n");
        writer.write("    \n");
    }
}
