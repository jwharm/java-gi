package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Bitfield extends ValueWrapper {

    public String getType;
    
    public Bitfield(GirElement parent, String name, String cType, String getType, String version) {
        super(parent, name, null, cType, version);
        this.getType = getType;
        
        // Generate a function declaration to retrieve the type of this bitfield.
        registerGetTypeFunction(getType);
    }

    public void generate(SourceWriter writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public class " + javaName + " extends io.github.jwharm.javagi.base.Bitfield {\n");
        writer.increaseIndent();

        generateMemoryLayout(writer);

        // Filter duplicate members
        for (Member m : filterDuplicates(memberList)) {
            if (m.usable) {
                writer.write("\n");
                if (m.doc != null) {
                    m.doc.generate(writer, false);
                }
                writer.write("public static final " + javaName + " " + m.name.toUpperCase() + " = new " + javaName + "("+ m.value + ");\n");
            } else {
                writer.write("// Skipped: " + m.name.toUpperCase() + "\n");
            }
        }
        
        generateValueConstructor(writer, "int");
        generateMethodsAndSignals(writer);
        
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Combine (bitwise OR) operation\n");
        writer.write(" * @param masks one or more values to combine with\n");
        writer.write(" * @return the combined value by calculating {@code this | mask} \n");
        writer.write(" */\n");
        writer.write("public " + javaName + " or(" + javaName + "... masks) {\n");
        if (!isApi()) {
            writer.write("    int value = this.getValue();\n");
            writer.write("    for (" + javaName + " arg : masks) {\n");
            writer.write("        value |= arg.getValue();\n");
            writer.write("    }\n");
            writer.write("    return new " + javaName + "(value);\n");
        } else writer.write("    throw Interop.apiError();\n");
        writer.write("}\n");
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Combine (bitwise OR) operation\n");
        writer.write(" * @param mask the first value to combine\n");
        writer.write(" * @param masks the other values to combine\n");
        writer.write(" * @return the combined value by calculating {@code mask | masks[0] | masks[1] | ...} \n");
        writer.write(" */\n");
        writer.write("public static " + javaName + " combined(" + javaName + " mask, " + javaName + "... masks) {\n");
        if (!isApi()) {
            writer.write("    int value = mask.getValue();\n");
            writer.write("    for (" + javaName + " arg : masks) {\n");
            writer.write("        value |= arg.getValue();\n");
            writer.write("    }\n");
            writer.write("    return new " + javaName + "(value);\n");
        } else writer.write("    throw Interop.apiError();\n");
        writer.write("}\n");

        generateDowncallHandles(writer);

        generateInjected(writer);

        writer.decreaseIndent();
        writer.write("}\n");
    }
    
    @Override
    public String getInteropString(String paramName, boolean isPointer) {
        String str = paramName + ".getValue()";
        if (isPointer) {
            return "new PointerInteger(" + str + ").handle()";
        } else {
            return str;
        }
    }

    private List<Member> filterDuplicates(List<Member> input) {
        HashSet<String> set = new HashSet<>();
        ArrayList<Member> output = new ArrayList<>();

        for (Member m : input) {
            if (! set.contains(m.name)) {
                output.add(m);
                set.add(m.name);
            }
        }
        return output;
    }
}
