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
    
    protected void generateMethodHandle(Writer writer) throws IOException {
        writer.write("    static final MethodHandle " + cIdentifier + " = Interop.downcallHandle(\n");
        writer.write("        \"" + cIdentifier + "\",\n");
        writer.write("        FunctionDescriptor.");
        if (returnValue.type == null || "void".equals(returnValue.type.simpleJavaType)) {
            writer.write("ofVoid(");
        } else {
            writer.write("of(" + Conversions.toPanamaMemoryLayout(returnValue.type));
            if (parameters != null) {
                writer.write(", ");
            }
        }
        if (parameters != null) {
            for (int i = 0; i < parameters.parameterList.size(); i++) {
                if (i > 0) {
                    writer.write(", ");
                }
                writer.write(Conversions.toPanamaMemoryLayout(parameters.parameterList.get(i).type));
            }
        }
        if (throws_ != null) {
        	writer.write(", ValueLayout.ADDRESS");
        }
        writer.write(")\n");
        writer.write("    );\n");
        writer.write("    \n");
    }

    public void generate(Writer writer, boolean isDefault, boolean isStatic) throws IOException {
        
        // Do not generate deprecated methods.
        if ("1".equals(deprecated)) {
            return;
        }
        
        generateMethodHandle(writer);

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
        
        writer.write("        try {\n");
        writer.write("            ");
        if (! (returnValue.type != null && returnValue.type.isVoid())) {
            writer.write("var RESULT = (");
            writer.write(Conversions.toPanamaJavaType(getReturnValue().type));
            writer.write(") ");
        }
        
        writer.write(cIdentifier + ".invokeExact");
        
        if (parameters != null) {
            writer.write("(");
            parameters.generateCParameters(writer, throws_);
            writer.write(")");
        } else {
            writer.write("()");
        }
        writer.write(";\n");
        
        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
                if (p.type != null && p.type.isAliasForPrimitive() && p.type.isPointer()) {
                    String typeStr = ((Alias) p.type.girElementInstance).type.simpleJavaType;
                    writer.write("            " + p.name + ".setValue(" + p.name + "POINTER.get());\n");
                }
            }
        }
        
        if (throws_ != null) {
            writer.write("            if (GErrorException.isErrorSet(GERROR)) {\n");
            writer.write("                throw new GErrorException(GERROR);\n");
            writer.write("            }\n");
        }
        
        returnValue.generateReturnStatement(writer, 3);
        
        writer.write("        } catch (Throwable ERR) {\n");
        writer.write("            throw new AssertionError(\"Unexpected exception occured: \", ERR);\n");
        writer.write("        }\n");
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
