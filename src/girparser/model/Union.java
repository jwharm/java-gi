package girparser.model;

import java.io.IOException;
import java.io.Writer;

public class Union extends RegisteredType {

    public Union(GirElement parent, String name) {
        super(parent, name, null);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);

        writer.write("import org.gtk.interop.NativeAddress;\n");
        writer.write("import jdk.incubator.foreign.MemoryAddress;\n");
        writer.write("\n");

        generateJavadoc(writer);

        writer.write("public class " + javaName + " extends org.gtk.interop.ResourceProxy {\n");
        writer.write("    \n");
        generateMemoryAddressConstructor(writer);
        writer.write("}\n");
        writer.write("\n");
    }
}
