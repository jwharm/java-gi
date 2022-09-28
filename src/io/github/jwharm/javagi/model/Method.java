package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import java.io.IOException;
import java.io.Writer;

public class Method extends GirElement implements CallableType {

    public final String cIdentifier, deprecated, throws_;
    public ReturnValue returnValue;
    public Parameters parameters;

    public Method(GirElement parent, String name, String cIdentifier, String deprecated, String throws_) {
        super(parent);
        this.name = name;
        this.cIdentifier = cIdentifier;
        this.deprecated = deprecated;
        this.throws_ = throws_;

        // Handle empty names. (For example, GLib.g_iconv is named "".)
        if ("".equals(name)) {
            this.name = cIdentifier;
        }
    }

    public void generate(Writer writer, boolean isDefault, boolean isStatic) throws IOException {
        boolean tryCatch = false;
        
        // Do not generate deprecated methods.
        if ("1".equals(deprecated)) {
            return;
        }

        writeMethodDeclaration(writer, doc, name, throws_, isDefault, isStatic);
        writer.write(" {\n");

        if (throws_ != null) {
            writer.write("        MemorySegment GERROR = Interop.getAllocator().allocate(ValueLayout.ADDRESS);\n");
        }
        
        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
                if (p.type != null && p.type.isAliasForPrimitive() && p.type.isPointer()) {
                    String typeStr = ((Alias) p.type.girElementInstance).type.simpleJavaType;
                    typeStr = Conversions.primitiveClassName(typeStr);
                    writer.write("        Pointer" + typeStr + " " + p.name + "POINTER = new Pointer" + typeStr + "(" + p.name + ".getValue());\n");
                }
            }
        }
        
        if (parameters != null && parameters.hasCallbackParameter()) {
            tryCatch = true;
            writer.write("        try {\n");
        }
        
        writer.write(" ".repeat(tryCatch ? 12 : 8));
        if (! (returnValue.type != null && returnValue.type.isVoid())) {
            writer.write("var RESULT = ");
        }
        writer.write("gtk_h." + cIdentifier);
        if (parameters != null) {
            writer.write("(");
            parameters.generateCParameters(writer, throws_);
            writer.write(")");
        } else {
            writer.write("()");
        }
        writer.write(";\n");
        
        if (throws_ != null) {
            writer.write(" ".repeat(tryCatch ? 12 : 8) + "if (GErrorException.isErrorSet(GERROR)) {\n");
            writer.write(" ".repeat(tryCatch ? 12 : 8) + "    throw new GErrorException(GERROR);\n");
            writer.write(" ".repeat(tryCatch ? 12 : 8) + "}\n");
        }
        
        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
                if (p.type != null && p.type.isAliasForPrimitive() && p.type.isPointer()) {
                    String typeStr = ((Alias) p.type.girElementInstance).type.simpleJavaType;
                    writer.write(" ".repeat(tryCatch ? 12 : 8) + p.name + ".setValue(" + p.name + "POINTER.get());\n");
                }
            }
        }
        
        returnValue.generateReturnStatement(writer, tryCatch ? 3 : 2);
        
        if (parameters != null && parameters.hasCallbackParameter()) {
            // NoSuchMethodException, IllegalAccessException from findStatic()
            // When the static callback methods have been successfully generated, these exceptions should never happen.
            // We can try to suppress them, but I think it's better to be upfront when they occur, and just crash
            // immediately so the stack trace will be helpful to solve the issue.
            writer.write("        } catch (IllegalAccessException | NoSuchMethodException e) {\n");
            writer.write("            throw new RuntimeException(e);\n");
            writer.write("        }\n");
        }
        writer.write("    }\n");
        writer.write("    \n");
    }

    @Override
    public Parameters getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Parameters ps) {
        this.parameters = ps;
    }

    @Override
    public ReturnValue getReturnValue() {
        return returnValue;
    }

    @Override
    public void setReturnValue(ReturnValue rv) {
        this.returnValue = rv;
    }
}
