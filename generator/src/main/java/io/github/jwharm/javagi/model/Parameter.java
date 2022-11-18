package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

import io.github.jwharm.javagi.generator.Conversions;

public class Parameter extends Variable {

    public final String transferOwnership, allowNone, direction;
    public final boolean nullable;

    public boolean varargs = false;

    public Parameter(GirElement parent, String name, String transferOwnership, String nullable, String allowNone, String direction) {
        super(parent);
        this.name = Conversions.toLowerCaseJavaName(name);
        this.transferOwnership = transferOwnership;
        this.nullable = "1".equals(nullable);
        this.allowNone = allowNone;
        this.direction = direction;
    }

    /**
     * Return the parameter that is referred to by index in a GIR attribute.
     * For example: array length="2" refers to the second parameter for the 
     * length of that particular array.
     * The index counts from zero and ignores the instance-parameter.
     */
    public Parameter getParameterAt(String indexAttr) {
        if (indexAttr == null) {
            return null;
        }
        // Parse the value of the 'length' attribute
        int index;
        try {
            index = Integer.parseInt(indexAttr);
        } catch (NumberFormatException nfe) {
            return null;
        }
        // Get the <parameters> node
        Parameters params;
        if (this instanceof ReturnValue) {
            params = ((CallableType) parent).getParameters();
        } else {
            params = (Parameters) parent;
        }
        // Find the parameter that was specified in the value of the 'length' attribute.
        // Ignore the instance parameter.
        if (params.parameterList.get(0) instanceof InstanceParameter) {
            return params.parameterList.get(index + 1);
        } else {
            return params.parameterList.get(index);
        }
    }
    
    public boolean isProxy() {
        if (type == null) {
            return false;
        }
        if (type.isAlias()) {
            Alias a = (Alias) type.girElementInstance;
            return (a == null || a.aliasFor() == Alias.CLASS_ALIAS) || (a.aliasFor() == Alias.INTERFACE_ALIAS);
        }
        return type.isClass() || type.isRecord() || type.isInterface() || type.isUnion();
    }
    
    /**
     * Returns an Ownership enum value (for example, tranfer-ownership="full" -> "Ownership.FULL").
     * If the transfer-ownership attribute is not set, Ownership.UNKNOWN is returned
     * @return a String containing the Ownership enum value
     */
    public String transferOwnership() {
        return "Ownership." + (transferOwnership == null ? "UNKNOWN" : transferOwnership.toUpperCase());
    }
    
    /**
     * Whether this parameter must receive special treatment as an out-parameter
     * @return True if the direction attribute exists and contains "out", AND the parameter type
     *         is NOT a Proxy object. (For Proxy object out-parameters, we can simply pas the 
     *         object's memory address, and don't need to do anything special.
     */
    public boolean isOutParameter() {
        return direction != null && direction.contains("out") && (! isProxy());
    }

    public boolean isInstanceParameter() {
        return (this instanceof InstanceParameter);
    }

    public boolean isCallbackParameter() {
        return (type != null) && type.isCallback();
    }

    public boolean isUserDataParameter() {
        return (type != null) && name.toLowerCase().endsWith("data")
                && ("gpointer".equals(type.cType) || "gconstpointer".equals(type.cType));
    }

    public boolean isDestroyNotify() {
        return isCallbackParameter() && "DestroyNotify".equals(type.simpleJavaType);
    }

    public boolean isErrorParameter() {
        return (type != null) && "GError**".equals(type.cType);
    }
    
    /**
     * We don't need to perform a null-check parameters that are not nullable, or not user-specified
     * (instance param, gerror, user_data or destroy_notify for callbacks), or primitive values.
     * @return true iff this parameter is nullable, is user-specified, and is not a primitive value
     */
    public boolean checkNull() {
        return nullable 
                && (! (isInstanceParameter() || isErrorParameter() || isUserDataParameter() || isDestroyNotify()
                        || (type != null && type.isPrimitive && (! type.isPointer()))));
    }
    
    /**
     * Generate code to do pre-processing of the parameter before the function call. This will 
     * generate a null check for NotNull parameters, and generate pointer allocation logic for 
     * pointer parameters.
     * @param writer The source code file writer
     * @param indent How many tabs to indent
     * @throws IOException Thrown when an error occurs while writing to the file
     */
    public void generatePreprocessing(Writer writer, int indent) throws IOException {
        
        // Generate null-check
        // Don't null-check parameters that are hidden from the Java API, or primitive values
        if (! (isInstanceParameter() || isErrorParameter() || isUserDataParameter() || isDestroyNotify() || varargs
                || (type != null && type.isPrimitive && (! type.isPointer())))) {
            if (! nullable) {
                writer.write(tab(indent) + "java.util.Objects.requireNonNull(" + name 
                        + ", \"" + "Parameter '" + name + "' must not be null\");\n");
            }
        }
        
        // Generate pointer allocation
        if (isOutParameter()) {
            writer.write(tab(indent) + "MemorySegment " + name + "POINTER = Interop.getAllocator().allocate(" + Conversions.getValueLayout(type) + ");\n");
            
        } else if (type != null && type.isAliasForPrimitive() && type.isPointer()) {
            String typeStr = type.girElementInstance.type.simpleJavaType;
            typeStr = Conversions.primitiveClassName(typeStr);
            writer.write(tab(indent) + "Pointer" + typeStr + " " + name + "POINTER = new Pointer" + typeStr + "(" + name + ".getValue());\n");
        }
    }
    
    /**
     * Generate code to do post-processing of the parameter after the function call.
     * @param writer The source code file writer
     * @param indent How many tabs to indent
     * @throws IOException Thrown when an error occurs while writing to the file
     */
    public void generatePostprocessing(Writer writer, int indent) throws IOException {
        if (isOutParameter()) {
            if (array == null) {
                // First the regular (non-array) out-parameters. These could include an out-parameter with 
                // the length of an array out-parameter, so we have to process these first.
                writer.write("        ");
                if (checkNull()) {
                    writer.write("if (" + name + " != null) ");
                }
                writer.write(name + ".set(");
                String identifier = name + "POINTER.get(" + Conversions.getValueLayout(type) + ", 0)";
                if (type.isPrimitive && type.isPointer()) {
                    writer.write(identifier);
                    if (type.isBoolean()) writer.write(" != 0");
                    writer.write(");\n");
                } else {
                    writer.write(getNewInstanceString(type, identifier, false) + ");\n");
                }
            } else {
                // Secondly, process the array out parameters
                String len = array.size();
                String valuelayout = Conversions.getValueLayout(array.type);
                if (array.type.isPrimitive && (! array.type.isBoolean())) {
                    // Array of primitive values
                    writer.write(tab(indent) + name + ".set(");
                    writer.write("MemorySegment.ofAddress(" + name + "POINTER.get(ValueLayout.ADDRESS, 0), " + len + " * " + valuelayout + ".byteSize(), Interop.getScope()).toArray(" + valuelayout + "));\n");
                } else {
                    // Array of proxy objects
                    writer.write(tab(indent) + array.type.qualifiedJavaType + "[] " + name + "ARRAY = new " + array.type.qualifiedJavaType + "[" + len + "];\n");
                    writer.write(tab(indent) + "for (int I = 0; I < " + len + "; I++) {\n");
                    writer.write(tab(indent + 1) + "var OBJ = " + name + "POINTER.get(" + valuelayout + ", I);\n");
                    writer.write(tab(indent + 1) + name + "ARRAY[I] = ");
                    writer.write(getNewInstanceString(array.type, "OBJ", false) + ";\n");
                    writer.write(tab(indent) + "}\n");
                    writer.write(tab(indent) + name + ".set(" + name + "ARRAY);\n");
                }
            }
        } else if (type != null && type.isAliasForPrimitive() && type.isPointer()) {
            writer.write(tab(indent + 1) + name + ".setValue(" + name + "POINTER.get());\n");
        }
        
        // If the parameter has attribute transfer-ownership="full", we don't need to unref it anymore.
        // Only for proxy objects where ownership is fully transferred away, unless it's an out parameter or a pointer.
        if (isProxy()
                && "full".equals(transferOwnership) 
                && (! isOutParameter()) 
                && (type.cType == null || (! type.cType.endsWith("**")))) {
            writer.write(tab(indent) + (isInstanceParameter() ? "this" : name) + ".yieldOwnership();\n");
        }
    }
}
