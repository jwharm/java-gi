package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

public class ReturnValue extends Parameter {

    public ReturnValue(GirElement parent, String transferOwnership) {
        super(parent, null, transferOwnership, null, null, null);
    }

    public void generateReturnStatement(Writer writer, int indent) throws IOException {
        if (type.isVoid()) {
            return;
        }

        writer.write(" ".repeat(indent * 4));

        if (type.simpleJavaType.equals("Type") || type.isAlias() && (!((Alias) type.girElementInstance).inherits())) {
            writer.write("return new " + type.qualifiedJavaType + "(RESULT);\n");
        } else if (type.isAlias() || type.isClass()) {
            writer.write("return new " + type.qualifiedJavaType + "(ProxyFactory.getProxy(RESULT, " + (transferOwnership() ? "true" : "false") + "));\n");
        } else if (type.isInterface()) {
            writer.write("return new " + type.qualifiedJavaType + "." + type.simpleJavaType + "Impl(ProxyFactory.getProxy(RESULT, " + (transferOwnership() ? "true" : "false") + "));\n");
        } else if (type.isEnum()) {
            writer.write("return " + type.qualifiedJavaType + ".fromValue(RESULT);\n");
        } else if (type.name.equals("gboolean") && (! type.cType.equals("_Bool"))) {
            // A gboolean corresponds to an int where value 0 is FALSE, and everything else is TRUE.
            writer.write("return (RESULT != 0);\n");
        } else if (type.qualifiedJavaType.equals("java.lang.String")) {
            writer.write("return RESULT.getUtf8String(0);\n");
        } else {
            writer.write("return RESULT;\n");
        }
    }
}
