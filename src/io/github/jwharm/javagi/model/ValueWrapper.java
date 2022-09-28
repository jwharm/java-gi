package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import java.io.IOException;
import java.io.Writer;

public abstract class ValueWrapper extends RegisteredType {
    
    public ValueWrapper(GirElement parent, String name, String parentClass, String cType) {
        super(parent, name, parentClass, cType);
    }
    
    public void generateValueConstructor(Writer writer, String typeStr) throws IOException {
        writer.write("    public " + javaName + "(" + typeStr + " value) {\n");
        writer.write("        super(value);\n");
        writer.write("    }\n");
        writer.write("    \n");
    }
    
    public String getInteropString(String paramName, boolean isPointer, boolean transferOwnership) {
        String str = paramName + ".getValue()";
        if (isPointer) {
            return "new Pointer" + Conversions.primitiveClassName(type.qualifiedJavaType) + "(" + str + ").handle()";
        } else {
            return str;
        }
    }
}
