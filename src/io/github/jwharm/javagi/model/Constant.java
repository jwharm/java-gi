package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

public class Constant extends GirElement {

    public final String value, cType;

    public Constant(GirElement parent, String name, String value, String cType) {
        super(parent);
        this.name = name;
        this.value = value;
        this.cType = cType;
    }

    public void generate(Writer writer) throws IOException {
        try {
            String printValue;
            if (type.isAliasForPrimitive()) {
                String aliasedType = ((Alias) type.girElementInstance).type.simpleJavaType;
                printValue = "new " + type.qualifiedJavaType + "(" + literal(aliasedType, value) + ")";
            } else if (type.isEnum()) {
                printValue = type.qualifiedJavaType + ".fromValue(" + literal("int", value) + ")";
            } else {
                printValue = literal(type.qualifiedJavaType, value);
            }
            writer.write("    public static final " + type.qualifiedJavaType + " " + name + " = " + printValue + ";\n");
            writer.write("\n");
        } catch (NumberFormatException nfe) {
            // Do not write anything
        }
    }

    private String literal(String type, String value) throws NumberFormatException {
        return switch (type) {
            case "boolean" -> Boolean.valueOf(value).toString();
            case "byte" -> Byte.valueOf(value).toString();
            case "char" -> "'" + value + "'";
            case "double" -> Double.valueOf(value) + "d";
            case "float" -> Float.valueOf(value) + "f";
            case "int" -> Integer.valueOf(value).toString();
            case "long" -> Long.valueOf(value) + "L";
            case "short" -> Short.valueOf(value).toString();
            case "java.lang.String" -> '"' + value + '"';
            default -> value;
        };
    }
}
