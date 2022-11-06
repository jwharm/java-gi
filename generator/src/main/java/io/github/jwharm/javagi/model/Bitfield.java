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

        generateDowncallHandles(writer);
        
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
