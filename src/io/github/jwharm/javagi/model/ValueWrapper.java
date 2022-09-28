package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import java.io.IOException;
import java.io.Writer;

public abstract class ValueWrapper extends RegisteredType {
    
    public ValueWrapper(GirElement parent, String name, String parentClass, String cType) {
        super(parent, name, parentClass, cType);
    }
    
    public void generateAccessors(Writer writer, String typeStr) throws IOException {
        writer.write("    private " + typeStr + " value;\n");
        writer.write("    \n");
        writer.write("    public " + javaName + "(" + typeStr + " value) {\n");
        writer.write("        this.value = value;\n");
        writer.write("    }\n");
        writer.write("    \n");
        writer.write("    public " + typeStr + " getValue() {\n");
        writer.write("        return this.value;\n");
        writer.write("    }\n");
        writer.write("    \n");
        writer.write("    public void setValue(" + typeStr + " value) {\n");
        writer.write("        this.value = value;\n");
        writer.write("    }\n");
        writer.write("    \n");
        writer.write("    public static " + typeStr + "[] getValues(" + javaName + "[] array) {\n");
        writer.write("        " + typeStr + "[] values = new " + typeStr + "[array.length];\n");
        writer.write("        for (int i = 0; i < array.length; i++) {\n");
        writer.write("            values[i] = array[i].getValue();\n");
        writer.write("        }\n");
        writer.write("        return values;\n");
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
