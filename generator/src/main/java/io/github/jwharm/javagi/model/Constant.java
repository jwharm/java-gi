package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

import io.github.jwharm.javagi.JavaGI;
import io.github.jwharm.javagi.generator.Conversions;

public class Constant extends GirElement {

    public final String value, cType;

    public Constant(GirElement parent, String name, String value, String cType) {
        super(parent);
        this.name = name;
        this.value = value;
        this.cType = cType;
    }

    public void generate(Writer writer) throws IOException {
        String typeStr = "unknown type";
        String printValue;
        try {
            if (type.isAliasForPrimitive()) {
                typeStr = type.girElementInstance.type.simpleJavaType;
                printValue = "new " + type.qualifiedJavaType + "(" + Conversions.literal(typeStr, value) + ")";
            } else if (type.isBitfield() || type.isEnum()) {
                typeStr = "int";
                printValue = "new " + type.qualifiedJavaType + "(" + Conversions.literal(typeStr, value) + ")";
            } else {
                typeStr = type.qualifiedJavaType;
                printValue = Conversions.literal(typeStr, value);
            }
        } catch (NumberFormatException nfe) {
            // Do not write anything
            System.out.println("Skipping <constant name=\"" + name + "\""
                    + " value=\"" + value + "\""
                    + ">: Value not allowed for " + typeStr);
            return;
        }
        
        writer.write("    \n");
        
        // Documentation
        if (doc != null) {
            doc.generate(writer, 1);
        }
        writer.write("    public static final " + type.qualifiedJavaType + " " + name + " = " + printValue + ";\n");
    }
}
