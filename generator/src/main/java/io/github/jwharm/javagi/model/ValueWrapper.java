package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import java.io.IOException;
import java.io.Writer;

public abstract class ValueWrapper extends RegisteredType {
    
    public ValueWrapper(GirElement parent, String name, String parentClass, String cType, String version) {
        super(parent, name, parentClass, cType, version);
    }
    
    public void generateValueConstructor(Writer writer, String typeStr) throws IOException {
        writer.write("    \n");
        writer.write("    public " + javaName + "(" + typeStr + " value) {\n");
        writer.write("        super(value);\n");
        writer.write("    }\n");
    }
    
    @Override
    public String getInteropString(String paramName, boolean isPointer, String transferOwnership) {
        String str = paramName + ".getValue()." + type.qualifiedJavaType + "Value()";
        if (isPointer) {
            return "new Pointer" + Conversions.primitiveClassName(type.qualifiedJavaType) + "(" + str + ").handle()";
        } else {
            return str;
        }
    }
}
