package girparser.model;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

public class Enumeration extends RegisteredType {

    public Enumeration(GirElement parent, String name) {
        super(parent, name, null);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);

        writer.write("import org.gtk.interop.NativeAddress;\n");
        writer.write("import jdk.incubator.foreign.MemoryAddress;\n");
        writer.write("\n");

        generateJavadoc(writer);

        writer.write("public enum " + javaName + " implements NativeAddress {\n");
        writer.write("\n");

        int remaining = memberList.size();
        for (Member m : memberList) {
            if (m.doc != null) {
                m.doc.generate(writer, 1);
            }
            writer.write("    " + m.name.toUpperCase());
            writer.write((--remaining > 0 ? "," : ";") + "\n");
            writer.write("    \n");
        }

        writer.write("    public static " + javaName + " fromValue(int value) {\n");
        writer.write("        return switch(value) {\n");

        // Filter duplicate values
        ArrayList<Integer> values = new ArrayList<>();
        for (Member m : memberList) {
            if (! values.contains(m.value)) {
                writer.write("            case " + m.value + " -> " + m.name.toUpperCase() + ";\n");
                values.add(m.value);
            }
        }
        writer.write("            default -> null;\n");
        writer.write("        };\n");
        writer.write("    }\n");
        writer.write("\n");

        writer.write("    public int getValue() {\n");
        writer.write("        return switch(this) {\n");
        for (Member m : memberList) {
            writer.write("            case " + m.name.toUpperCase() + " -> " + m.value + ";\n");
        }
        writer.write("        };\n");
        writer.write("    }\n");
        writer.write("\n");

        writer.write("    private MemoryAddress __HANDLE__;\n");
        writer.write("\n");
        writer.write("    @Override\n");
        writer.write("    public void setHANDLE(MemoryAddress handle) {\n");
        writer.write("        this.__HANDLE__ = handle;\n");
        writer.write("}\n");
        writer.write("\n");
        writer.write("    @Override\n");
        writer.write("    public MemoryAddress HANDLE() {\n");
        writer.write("        return __HANDLE__;\n");
        writer.write("    }\n");
        writer.write("}\n");
    }
}
