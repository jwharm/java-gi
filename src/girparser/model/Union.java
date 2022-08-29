package girparser.model;

import java.io.IOException;
import java.io.Writer;

public class Union extends RegisteredType {

    public Union(GirElement parent, String name) {
        super(parent, name);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);

        writer.write("import org.gtk.interop.NativeAddress;\n");
        writer.write("import jdk.incubator.foreign.MemoryAddress;\n");
        writer.write("\n");

        generateJavadoc(writer);

        writer.write("public class " + javaName + " implements NativeAddress {\n");

        writer.write("    private MemoryAddress __HANDLE__;\n");
        writer.write("\n");
        writer.write("    @Override\n");
        writer.write("    public void setHANDLE(MemoryAddress handle) {\n");
        writer.write("        this.__HANDLE__ = handle;\n");
        writer.write("    }\n");
        writer.write("\n");
        writer.write("    @Override\n");
        writer.write("    public MemoryAddress HANDLE() {\n");
        writer.write("        return __HANDLE__;\n");
        writer.write("    }\n");
        writer.write("}\n");
        writer.write("\n");
    }
}
