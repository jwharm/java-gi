package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

public class Bitfield extends ValueWrapper {

    public Bitfield(GirElement parent, String name, String cType, String version) {
        super(parent, name, null, cType, version);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public class " + javaName + " extends io.github.jwharm.javagi.Bitfield {\n");
        
        generateCType(writer);
        generateMemoryLayout(writer);
        
        for (Member m : memberList) {
            writer.write("    \n");
            if (m.doc != null) {
                m.doc.generate(writer, 1);
            }
            writer.write("    public static final " + javaName + " " + m.name.toUpperCase() + " = new " + javaName + "("+ m.value + ");\n");
        }
        
        generateValueConstructor(writer, "int");
        
        for (Function function : functionList) {
            function.generate(writer, false, true);
        }
        
        writer.write(
                  "    \n"
                + "    /**\n"
                + "     * Combine (bitwise OR) operation\n"
                + "     * @param mask the value to combine with\n"
                + "     * @return the combined value by calculating {@code this | mask} \n"
                + "     */\n"
                + "    public " + javaName + " combined(" + javaName + " mask) {\n"
                + "        this.setValue(this.getValue() | mask.getValue());\n"
                + "        return this;\n"
                + "    }\n"
                + "    \n"
                + "    /**\n"
                + "     * Combine (bitwise OR) operation\n"
                + "     * @param mask the first value to combine\n"
                + "     * @param masks the other values to combine\n"
                + "     * @return the combined value by calculating {@code mask | masks[0] | masks[1] | ...} \n"
                + "     */\n"
                + "    public static " + javaName + " combined(" + javaName + " mask, " + javaName + "... masks) {\n"
                + "        for (" + javaName + " arg : masks) {\n"
                + "            mask.setValue(mask.getValue() | arg.getValue());\n"
                + "        }\n"
                + "        return mask;\n"
                + "    }\n"
        );

        generateDowncallHandles(writer);
        
        writer.write("}\n");
    }
    
    @Override
    public String getInteropString(String paramName, boolean isPointer, String transferOwnership) {
        String str = paramName + ".getValue()";
        if (isPointer) {
            return "new PointerInteger(" + str + ").handle()";
        } else {
            return str;
        }
    }
}
