package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;

public abstract class ValueWrapper extends RegisteredType {
    
    public ValueWrapper(GirElement parent, String name, String parentClass, String cType, String version) {
        super(parent, name, parentClass, cType, version);
    }
    
    public void generateValueConstructor(SourceWriter writer, String typeStr) throws IOException {
        writer.write("\n");
        writer.write("/**\n");
        writer.write(" * Create a new " + javaName + " with the provided value\n");
        writer.write(" */\n");
        writer.write("public " + javaName + "(" + typeStr + " value) {\n");
        writer.write("    super(value);\n");
        writer.write("}\n");
    }
    
    @Override
    public String getInteropString(String paramName, boolean isPointer) {
        String str = paramName + ".getValue()." + type.qualifiedJavaType + "Value()";
        if ("java.lang.foreign.MemoryAddress".equals(type.qualifiedJavaType)) {
            str = "(Addressable) " + paramName + ".getValue()";
        }
        if (isPointer) {
            return "new Pointer" + Conversions.primitiveClassName(type.qualifiedJavaType) + "(" + str + ").handle()";
        } else {
            return str;
        }
    }
}
