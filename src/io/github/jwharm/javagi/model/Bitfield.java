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

        writer.write("public class " + javaName + " {\n");
        writer.write("\n");
        
        for (Member m : memberList) {
            if (m.doc != null) {
                m.doc.generate(writer, 1);
            }
            writer.write("    public static final " + javaName + " " + m.name.toUpperCase() + " = new " + javaName + "("+ m.value + ");\n");
            writer.write("    \n");
        }
        
        generateAccessors(writer, "int");
        
        writer.write("    public " + javaName + " combined(" + javaName + " mask) {\n");
        writer.write("        return new " + javaName + "(this.getValue() | mask.getValue());\n");
        writer.write("    }\n");
        writer.write("    \n");
        writer.write("    public static " + javaName + " combined(" + javaName + " mask, " + javaName + "... masks) {\n");
        writer.write("        int value = mask.getValue();\n");
        writer.write("        for (" + javaName + " arg : masks) {\n");
        writer.write("            value |= arg.getValue();\n");
        writer.write("        }\n");
        writer.write("        return new " + javaName + "(value);\n");
        writer.write("    }\n");
        writer.write("    \n");
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
