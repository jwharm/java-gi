package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

import io.github.jwharm.javagi.generator.Conversions;

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
    
    protected void generateMethodHandle(Writer writer, boolean isInterface) throws IOException {
        if (isInterface) writer.write("    @ApiStatus.Internal static final ");
        else writer.write("    private static final ");
        writer.write("MethodHandle " + cIdentifier + " = Interop.downcallHandle(\n");
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

    public void generate(Writer writer, boolean isInterface, boolean isStatic) throws IOException {
        
        // Do not generate deprecated methods.
        if ("1".equals(deprecated)) {
            return;
        }
        
        generateMethodHandle(writer, isInterface);

        writeMethodDeclaration(writer, doc, name, throws_, isInterface, isStatic);
        writer.write(" {\n");

        if (throws_ != null) {
            writer.write("        MemorySegment GERROR = Interop.getAllocator().allocate(ValueLayout.ADDRESS);\n");
        }
        
        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
            	if (p.isOutParameter()) {
            		writer.write("        MemorySegment " + p.name + "POINTER = Interop.getAllocator().allocate(" + Conversions.getValueLayout(p.type) + ");\n");
            	} else  if (p.type != null && p.type.isAliasForPrimitive() && p.type.isPointer()) {
                    String typeStr = ((Alias) p.type.girElementInstance).type.simpleJavaType;
                    typeStr = Conversions.primitiveClassName(typeStr);
                    writer.write("        Pointer" + typeStr + " " + p.name + "POINTER = new Pointer" + typeStr + "(" + p.name + ".getValue());\n");
                }
            }
        }
        
        if (! (returnValue.type != null && returnValue.type.isVoid())) {
        	writer.write("        " + Conversions.toPanamaJavaType(getReturnValue().type) + " RESULT;\n");
        }
        writer.write("        try {\n");
        writer.write("            ");
        if (! (returnValue.type != null && returnValue.type.isVoid())) {
            writer.write("RESULT = (");
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
        writer.write("        } catch (Throwable ERR) {\n");
        writer.write("            throw new AssertionError(\"Unexpected exception occured: \", ERR);\n");
        writer.write("        }\n");
        
        // Non-array out parameters
        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
            	if (p.isOutParameter()) {
            		if (p.array == null) {
                		writer.write("        " + p.name + ".set(");
                		String identifier = p.name + "POINTER.get(" + Conversions.getValueLayout(p.type) + ", 0)";
                		writer.write(p.getNewInstanceString(p.type, identifier) + ");\n");
            		}
            	} else if (p.type != null && p.type.isAliasForPrimitive() && p.type.isPointer()) {
                    writer.write("            " + p.name + ".setValue(" + p.name + "POINTER.get());\n");
                }
            }
        }
        // Array out parameters
        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
            	if (p.isOutParameter()) {
            		if (p.array != null) {
            			String len = p.array.size();
            			String valuelayout = Conversions.getValueLayout(p.array.type);
            			if (p.array.type.isPrimitive && (! p.array.type.isBoolean())) {
            				// Array of primitive values
                    		writer.write("        " + p.name + ".set(");
                			writer.write("MemorySegment.ofAddress(" + p.name + "POINTER.get(ValueLayout.ADDRESS, 0), " + len + " * " + valuelayout + ".byteSize(), Interop.getScope()).toArray(" + valuelayout + "));\n");
            			} else {
            				// Array of proxy objects
            				writer.write("        " + p.array.type.qualifiedJavaType + "[] " + p.name + "ARRAY = new " + p.array.type.qualifiedJavaType + "[" + len + "];\n");
            				writer.write("        for (int I = 0; I < " + len + "; I++) {\n");
            				writer.write("            var OBJ = " + p.name + "POINTER.get(" + valuelayout + ", I);\n");
            				writer.write("            " + p.name + "ARRAY[I] = ");
            	            writer.write(p.getNewInstanceString(p.array.type, "OBJ") + ";\n");
            				writer.write("        }\n");
            				writer.write("        " + p.name + ".set(" + p.name + "ARRAY);\n");
            			}
            		}
                }
            }
        }
        
        if (throws_ != null) {
            writer.write("        if (GErrorException.isErrorSet(GERROR)) {\n");
            writer.write("            throw new GErrorException(GERROR);\n");
            writer.write("        }\n");
        }
        
        if (returnValue.array != null) {
        	String len = returnValue.array.size();
        	if (len != null) {
    			String valuelayout = Conversions.getValueLayout(returnValue.array.type);
    			if (returnValue.array.type.isPrimitive && (! returnValue.array.type.isBoolean())) {
    				// Array of primitive values
            		writer.write("        return MemorySegment.ofAddress(RESULT.get(ValueLayout.ADDRESS, 0), " + len + " * " + valuelayout + ".byteSize(), Interop.getScope()).toArray(" + valuelayout + ");\n");
    			} else {
    				// Array of proxy objects
    				writer.write("        " + returnValue.array.type.qualifiedJavaType + "[] resultARRAY = new " + returnValue.array.type.qualifiedJavaType + "[" + len + "];\n");
    				writer.write("        for (int I = 0; I < " + len + "; I++) {\n");
    				writer.write("            var OBJ = RESULT.get(" + valuelayout + ", I);\n");
    				writer.write("            resultARRAY[I] = " + returnValue.getNewInstanceString(returnValue.array.type, "OBJ") + ";\n");
    				writer.write("        }\n");
    				writer.write("        return resultARRAY;\n");
    			}
        	} else {
                returnValue.generateReturnStatement(writer, 2);
        	}
        } else {
            returnValue.generateReturnStatement(writer, 2);
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
