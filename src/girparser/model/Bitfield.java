package girparser.model;

import java.io.IOException;
import java.io.Writer;

// Not implemented
public class Bitfield extends RegisteredType {

    public Bitfield(GirElement parent, String name) {
        super(parent, name);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateJavadoc(writer);

        writer.write("public class " + javaName + " {\n");
        writer.write("\n");

        for (Member m : memberList) {
            if (m.doc != null) {
                m.doc.generate(writer, 1);
            }
            writer.write("    public static final int " + m.name.toUpperCase() + " = " + m.value + ";\n");
            writer.write("    \n");
        }
        writer.write("}\n");
    }
}
