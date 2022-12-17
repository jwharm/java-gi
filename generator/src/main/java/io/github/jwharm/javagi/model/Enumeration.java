package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

public class Enumeration extends ValueWrapper {

    public Enumeration(GirElement parent, String name, String cType, String version) {
        super(parent, name, null, cType, version);
    }

    public void generate(Writer writer) throws IOException {
        generatePackageDeclaration(writer);
        generateImportStatements(writer);
        generateJavadoc(writer);

        writer.write("public enum " + javaName + " implements io.github.jwharm.javagi.Enumeration {\n");

        // Some enumerations contain duplicate members or members with invalid values
        // This filters them from inclusion as enum members.
        // Duplicates are added as public static final fields below
        List<Member> usable = memberList.stream()
                .filter(s -> s.usable)
                .collect(Collectors.groupingBy(s -> s.value, LinkedHashMap::new, Collectors.reducing((l, r) -> l)))
                .values().stream()
                .flatMap(Optional::stream)
                .toList();
        Member lastUsable = usable.get(usable.size() - 1);
        for (Member m : memberList) {
            if (usable.contains(m)) {
                if (m.doc != null) m.doc.generate(writer, 1);
                writer.write("    " + m.name.toUpperCase() + "(" + m.value + ")");
                if (m == lastUsable) writer.write(";\n");
                else writer.write(",\n");
            } else if (!m.usable) {
                writer.write("    // Skipped " + m.name.toUpperCase() + "(" + m.value + ")\n");
            }
        }

        // Add usable but duplicate members as public static final fields pointing to the member with the same value
        for (Member m : memberList) {
            if (m.usable && !usable.contains(m)) {
                Member u = null;
                for (Member u1 : usable) {
                    if (u1.value == m.value) u = u1;
                }
                if (u == null) System.out.println("Could not get corresponding enum member for " + m.name.toUpperCase());
                else writer.write("    public static final " + javaName + " " + m.name.toUpperCase() + " = " + u.name.toUpperCase() + ";\n");
            }
        }

        generateCType(writer);
        generateMemoryLayout(writer);

        writer.write("    \n");
        writer.write("    private final int value;\n");
        writer.write("    " + javaName + "(int value) {\n");
        writer.write("        this.value = value;\n");
        writer.write("    }\n");
        writer.write("    \n");
        writer.write("    @Override\n");
        writer.write("    public int getValue() {\n");
        writer.write("        return value;\n");
        writer.write("    }\n");
        writer.write("    \n");
        writer.write("    public static " + javaName + " of(int value) {\n");
        writer.write("        return switch (value) {\n");
        for (Member m : usable) {
            writer.write("            case " + m.value + " -> " + m.name.toUpperCase() + ";\n");
        }
        writer.write("            default -> throw new IllegalStateException(\"Unexpected value: \" + value);\n");
        writer.write("        };\n");
        writer.write("    }\n");
        
        for (Function function : functionList) {
            function.generate(writer, false, true);
        }

        generateDowncallHandles(writer);

        generateInjected(writer);
        
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
