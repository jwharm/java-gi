package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

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
        writer.write("    \n");
        
        // Documentation
        if (doc != null) {
            doc.generate(writer, 1);
        }
        
        try {
            String printValue;
            if (type.isAliasForPrimitive()) {
                String aliasedType = ((Alias) type.girElementInstance).type.simpleJavaType;
                printValue = "new " + type.qualifiedJavaType + "(" + Conversions.literal(aliasedType, value) + ")";
            } else if (type.isEnum()) {
                printValue = type.qualifiedJavaType + ".fromValue(" + Conversions.literal("int", value) + ")";
            } else {
                printValue = Conversions.literal(type.qualifiedJavaType, value);
            }
            writer.write("    public static final " + type.qualifiedJavaType + " " + name + " = " + printValue + ";\n");
        } catch (NumberFormatException nfe) {
            // Do not write anything
        }
    }
}
