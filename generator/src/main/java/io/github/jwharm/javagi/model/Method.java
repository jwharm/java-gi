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
    
    public void generateMethodHandle(Writer writer, boolean isInterface) throws IOException {
        boolean varargs = false;
        writer.write("        \n");
        writer.write("        ");
    	writer.write(isInterface ? "@ApiStatus.Internal\n        " : "private ");
        writer.write("static final MethodHandle " + cIdentifier + " = Interop.downcallHandle(\n");
        writer.write("            \"" + cIdentifier + "\",\n");
        writer.write("            FunctionDescriptor.");
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
                if (parameters.parameterList.get(i).varargs) {
                    varargs = true;
                    break;
                }
                if (i > 0) {
                    writer.write(", ");
                }
                writer.write(Conversions.toPanamaMemoryLayout(parameters.parameterList.get(i).type));
            }
        }
        if (throws_ != null) {
            writer.write(", ValueLayout.ADDRESS");
        }
        writer.write("),\n");
        writer.write(varargs ? "            true\n" : "            false\n");
        writer.write("        );\n");
    }

    protected void generateNullParameterChecks(Writer writer) throws IOException {
        if (parameters != null) {
        	for (Parameter p : parameters.parameterList) {
        		// Don't null-check parameters that are hidden from the Java API, or primitive values
        		if (! (p.isInstanceParameter() || p.isErrorParameter() || p.isUserDataParameter() || p.isDestroyNotify() || p.varargs
        				|| (p.type != null && p.type.isPrimitive && (! p.type.isPointer())))) {
        			if (! p.nullable) {
            			writer.write("        java.util.Objects.requireNonNull(" + p.name 
            					+ ", \"" + "Parameter '" + p.name + "' must not be null\");\n");
        			}
        		}
        	}
        }
    }

    public void generate(Writer writer, boolean isInterface, boolean isStatic) throws IOException {
        
        writer.write("    \n");
        writeMethodDeclaration(writer, doc, name, throws_, isInterface, isStatic);
        writer.write(" {\n");
        
        if (! isSafeToBind()) {
        	writer.write("        throw new UnsupportedOperationException(\"Operation not supported yet\");\n");
            writer.write("    }\n");
            return;
        }
        
        // Generate checks for null parameters
        generateNullParameterChecks(writer);

        if (throws_ != null) {
            writer.write("        MemorySegment GERROR = Interop.getAllocator().allocate(ValueLayout.ADDRESS);\n");
        }
        
        // MemorySegment declarations for pointer parameters
        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
                if (p.isOutParameter()) {
                    writer.write("        MemorySegment " + p.name + "POINTER = Interop.getAllocator().allocate(" + Conversions.getValueLayout(p.type) + ");\n");
                } else if (p.type != null && p.type.isAliasForPrimitive() && p.type.isPointer()) {
                    String typeStr = p.type.girElementInstance.type.simpleJavaType;
                    typeStr = Conversions.primitiveClassName(typeStr);
                    writer.write("        Pointer" + typeStr + " " + p.name + "POINTER = new Pointer" + typeStr + "(" + p.name + ".getValue());\n");
                }
            }
        }

        String panamaReturnType = Conversions.toPanamaJavaType(getReturnValue().type);
        if (! (returnValue.type != null && returnValue.type.isVoid())) {
            writer.write("        " + panamaReturnType + " RESULT;\n");
        }
        writer.write("        try {\n");
        writer.write("            ");
        if (! (returnValue.type != null && returnValue.type.isVoid())) {
            writer.write("RESULT = (");
            writer.write(panamaReturnType);
            writer.write(") ");
        }
        
        writer.write("DowncallHandles." + cIdentifier + ".invokeExact");
        
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

        // Throw GErrorException
        if (throws_ != null) {
            writer.write("        if (GErrorException.isErrorSet(GERROR)) {\n");
            writer.write("            throw new GErrorException(GERROR);\n");
            writer.write("        }\n");
        }
        
        // Read pointer values from memory segments
        if (parameters != null) {
            // Non-array out parameters
            for (Parameter p : parameters.parameterList) {
            	if (p.isOutParameter()) {
            		if (p.array == null) {
                		writer.write("        ");
            			if (p.checkNull()) {
            				writer.write("if (" + p.name + " != null) ");
            			}
                		writer.write(p.name + ".set(");
                		String identifier = p.name + "POINTER.get(" + Conversions.getValueLayout(p.type) + ", 0)";
                		if (p.type.isPrimitive && p.type.isPointer()) {
                    		writer.write(identifier);
                    		if (p.type.isBoolean()) writer.write(" != 0");
                    		writer.write(");\n");
                		} else {
                    		writer.write(p.getNewInstanceString(p.type, identifier, false) + ");\n");
                		}
            		}
            	} else if (p.type != null && p.type.isAliasForPrimitive() && p.type.isPointer()) {
                    writer.write("            " + p.name + ".setValue(" + p.name + "POINTER.get());\n");
                }
            }
            // Array out parameters
            for (Parameter p : parameters.parameterList) {
            	if (p.isOutParameter() && p.array != null) {
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
        	            writer.write(p.getNewInstanceString(p.array.type, "OBJ", false) + ";\n");
        				writer.write("        }\n");
        				writer.write("        " + p.name + ".set(" + p.name + "ARRAY);\n");
        			}
                }
            }
        }
        
        if (returnValue.array != null) {
        	String len = returnValue.array.size();
        	if (len != null) {
                if (getReturnValue().nullable) {
                    switch (panamaReturnType) {
                        case "MemoryAddress" -> writer.write("        if (RESULT.equals(MemoryAddress.NULL)) return null;\n");
                        case "MemorySegment" -> writer.write("        if (RESULT.address().equals(MemoryAddress.NULL)) return null;\n");
                        default -> System.err.println("Unexpected nullable return type: " + panamaReturnType);
                    }
                }
                String valuelayout = Conversions.getValueLayout(returnValue.array.type);
    			if (returnValue.array.type.isPrimitive && (! returnValue.array.type.isBoolean())) {
    				// Array of primitive values
            		writer.write("        return MemorySegment.ofAddress(RESULT.get(ValueLayout.ADDRESS, 0), " + len + " * " + valuelayout + ".byteSize(), Interop.getScope()).toArray(" + valuelayout + ");\n");
    			} else {
    				// Array of proxy objects
    				writer.write("        " + returnValue.array.type.qualifiedJavaType + "[] resultARRAY = new " + returnValue.array.type.qualifiedJavaType + "[" + len + "];\n");
    				writer.write("        for (int I = 0; I < " + len + "; I++) {\n");
    				writer.write("            var OBJ = RESULT.get(" + valuelayout + ", I);\n");
    				writer.write("            resultARRAY[I] = " + returnValue.getNewInstanceString(returnValue.array.type, "OBJ", false) + ";\n");
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
