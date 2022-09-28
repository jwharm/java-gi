package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

public class Enumeration extends ValueWrapper {

    public Enumeration(GirElement parent, String name, String cType) {
        super(parent, name, null, cType);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateJavadoc(writer);

        writer.write("public class " + javaName + " {\n");
        writer.write("\n");

        ArrayList<Integer> values = new ArrayList<>();
        for (Member m : memberList) {
            if (! values.contains(m.value)) {
                if (m.doc != null) {
                    m.doc.generate(writer, 1);
                }
                writer.write("    public static final " + javaName + " " 
                        + m.name.toUpperCase() + " = new " + javaName + "(" + m.value + ");\n");
                writer.write("    \n");
            }
        }
        
        generateAccessors(writer, "int");
        writer.write("}\n");
    }
    
    public String getInteropString(String paramName, boolean isPointer, boolean transferOwnership) {
        String str = paramName + ".getValue()";
        if (isPointer) {
            return "new PointerInteger(" + str + ").handle()";
        } else {
            return str;
        }
    }
}
