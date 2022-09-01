package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

public class Alias extends RegisteredType {

    public Alias(GirElement parent, String name) {
        super(parent, name, null);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        writer.write("import jdk.incubator.foreign.*;\n");

        generateJavadoc(writer);

        writer.write("public class " + javaName);

        // Handle alias for type "none"
        if (type.qualifiedJavaType.equals("void")) {
            writer.write(" extends org.gtk.gobject.Object");
        } else if (inherits()) {
            writer.write(" extends " + type.qualifiedJavaType);
        }
        writer.write(" {\n");
        writer.write("\n");

        // Generate standard constructors from a MemoryAddress and a gobject.Object
        if (inherits()) {
            // A record (C Struct) is not a GObject
            if ((type.girElementInstance != null)
                    && (type.girElementInstance.type != null)
                    && (! type.girElementInstance.type.isRecord())) {
                generateCastFromGObject(writer);
            }
            generateMemoryAddressConstructor(writer);
        } else {
            writer.write("    private final " + type.simpleJavaType + " value;\n");
            writer.write("    \n");
            writer.write("    public " + javaName + "(" + type.simpleJavaType + " value) {\n");
            writer.write("        this.value = value;\n");
            writer.write("    }\n");
            writer.write("    \n");
            writer.write("    public " + type.simpleJavaType + " getValue() {\n");
            writer.write("        return this.value;\n");
            writer.write("    }\n");
            writer.write("    \n");
        }
        writer.write("}\n");
    }

    // Aliases (typedefs) don't exist in Java. We can emulate this using inheritance.
    // For primitives and Strings, we wrap the value.
    public boolean inherits() {
        return (! (type.isPrimitive
                || type.isCallback()
                || type.qualifiedJavaType.equals("java.lang.String")));
    }
}
