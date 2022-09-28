package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

// Not implemented
public class Bitfield extends ValueWrapper {

    public Bitfield(GirElement parent, String name, String cType) {
        super(parent, name, null, cType);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateJavadoc(writer);

        writer.write("public class " + javaName + " extends io.github.jwharm.javagi.Bitfield {\n");
        writer.write("\n");
        
        for (Member m : memberList) {
            if (m.doc != null) {
                m.doc.generate(writer, 1);
            }
            writer.write("    public static final " + javaName + " " + m.name.toUpperCase() + " = new " + javaName + "("+ m.value + ");\n");
            writer.write("    \n");
        }
        
        generateValueConstructor(writer, "int");
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
