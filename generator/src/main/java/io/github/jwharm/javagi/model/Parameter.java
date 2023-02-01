package io.github.jwharm.javagi.model;

import java.io.IOException;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.SourceWriter;

public class Parameter extends Variable {

    public final String transferOwnership;
    public final boolean nullable;
    public final boolean notnull;
    public final String direction;
    public final String closure;

    public boolean varargs = false;

    /**
     * An array with a "length" attribute which refers to this parameter.
     */
    public Array linkedArray; // This field is initialized in the CrossReference class.

    public Parameter(GirElement parent, String name, String transferOwnership, String nullable,
                     String allowNone, String optional, String direction, String closure) {

        super(parent);
        this.name = Conversions.toLowerCaseJavaName(name);
        this.transferOwnership = transferOwnership;
        this.nullable = "1".equals(nullable) || "1".equals(allowNone) || "1".equals(optional);
        this.notnull = "0".equals(nullable) || "0".equals(allowNone) || "0".equals(optional);
        this.direction = direction;
        this.closure = closure;
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
        Parameter p0 = params.parameterList.get(0);
        if (p0 instanceof InstanceParameter) {
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
            return (a == null || a.getTargetType() == Alias.TargetType.CLASS) || (a.getTargetType() == Alias.TargetType.INTERFACE);
        }
        // A pointer to a proxy is not a proxy
        if (type.cType != null && type.cType.endsWith("**")) {
            return false;
        }
        return type.isClass() || type.isRecord() || type.isInterface() || type.isUnion();
    }

    public boolean isAliasForPrimitive() {
        return type != null && type.isAliasForPrimitive();
    }

    /**
     * Whether this parameter must receive special treatment as an out-parameter
     * @return True if the direction attribute exists and contains "out", AND the parameter type
     *         is NOT a Proxy object, a primitive alias, or an array with unknown size. (For Proxy
     *         objects, we can simply pass the memory address, and don't need to do anything
     *         special. For aliases, we can pass the alias object. Arrays with unknown size are
     *         already marshalled to Pointer objects.
     */
    public boolean isOutParameter() {
        if (array != null && array.size(false) == null)
            return false;

        return direction != null && direction.contains("out")
                && (type == null || type.isPointer() || (type.cType != null && type.cType.endsWith("gsize")))
                && (!isProxy())
                && (!isAliasForPrimitive());
    }

    public boolean isInstanceParameter() {
        return (this instanceof InstanceParameter);
    }

    public boolean isUserDataParameter() {
        // Closure parameters: the user_data parameter has attribute "closure" set
        if (parent.parent instanceof Closure) {
            return (closure != null);
        }

        // Method parameters that pass a user_data pointer to a closure
        else {
            // A user_data parameter must have a type (pointer)
            if (type == null)
                return false;

            // The c:type must be a gpointer or gconstpointer
            if (! ("gpointer".equals(type.cType) || "gconstpointer".equals(type.cType)))
                return false;

            // Loop through the callback parameter(s)
            Parameters parameters = (Parameters) parent;
            for (Parameter p : parameters.parameterList) {
                if (p.type != null && p.type.isCallback()) {

                    // The closure attribute points to the user_data parameter
                    if (p.closure != null) {
                        Parameter userData = getParameterAt(p.closure);
                        // Is this the user_data parameter that is referred to?
                        return userData.name.equals(this.name);
                    }

                }
            }
            return false;
        }
    }

    public boolean isErrorParameter() {
        return (type != null) && "GError**".equals(type.cType);
    }

    public boolean isArrayLengthParameter() {
        return linkedArray != null;
    }
    
    /**
     * We don't need to perform a null-check on parameters that are not nullable, or not user-specified
     * (instance param, gerror, user_data or array length), or varargs, bitfields, enums, or primitive values/aliases.
     * @return true iff this parameter is nullable, is user-specified, and is not a primitive value
     */
    public boolean checkNull() {
        return (!notnull)
                && (! (isInstanceParameter() || isErrorParameter() || isUserDataParameter() || isArrayLengthParameter()
                        || varargs
                        || (type != null && (! type.isPointer()) && (type.isPrimitive ||
                                                                     type.isAliasForPrimitive() ||
                                                                     type.isBitfield() ||
                                                                     type.isEnum()))));
    }

    /**
     * Generate code to do pre-processing of the parameter before the function call. This will 
     * generate a null check for NotNull parameters, and generate pointer allocation logic for 
     * pointer parameters.
     * @param writer The source code file writer
     * @throws IOException Thrown when an error occurs while writing to the file
     */
    public void generatePreprocessing(SourceWriter writer) throws IOException {
        
        // Generate null-check
        // Don't null-check parameters that are hidden from the Java API, or primitive values
        if (! (isInstanceParameter() || isErrorParameter() || isUserDataParameter() || isArrayLengthParameter() || varargs
                || (type != null && type.isPrimitive && (! type.isPointer())))) {
            if (notnull) {
                writer.write("java.util.Objects.requireNonNull(" + name
                        + ", \"" + "Parameter '" + name + "' must not be null\");\n");
            }
        }
        
        // Generate pointer allocation
        if (isOutParameter() && array != null && array.size(false) != null) {
            writer.write("MemorySegment " + name + "POINTER = (MemorySegment) ");
            marshalJavaToNative(writer, name + ".get()", false, false);
            writer.write(";\n");
        } else if (isOutParameter() || (isAliasForPrimitive() && type.isPointer())) {
            writer.write("MemorySegment " + name + "POINTER = SCOPE.allocate(" + Conversions.getValueLayout(type) + ");\n");
        }

        // Array length parameter: generate local variable that contains the length
        // array length
        if (isArrayLengthParameter()) {
            writeTypeAndName(writer, false);
            writer.write (" = ");
            if (isOutParameter()) {
                writer.write("new Out<>();\n");
            } else {
                Parameter arrayParam = (Parameter) linkedArray.parent;
                String arrayLength = arrayParam.name + ".length";

                // Force lossy conversion if needed
                String cast = "";
                if (type != null) {
                    String javaType = type.qualifiedJavaType;
                    if ("byte".equals(javaType) || "short".equals(javaType)) {
                        cast = "(" + javaType + ") ";
                    }
                }

                // For out parameter arrays, generate "arg.get().length", with null-checks for both arg and arg.get()
                if (arrayParam.isOutParameter())
                    arrayLength = arrayParam.name + ".get() == null ? 0 : " + cast + arrayParam.name + ".get().length";

                writer.write(arrayParam.name + " == null ? 0 : " + cast + arrayLength + ";\n");
            }
        }

    }
    
    /**
     * Generate code to do post-processing of the parameter after the function call.
     * @param writer The source code file writer
     * @throws IOException Thrown when an error occurs while writing to the file
     */
    public void generatePostprocessing(SourceWriter writer) throws IOException {
        if (isOutParameter() || (isAliasForPrimitive() && type.isPointer())) {
            if (array == null) {
                // First the regular (non-array) out-parameters. These could include an out-parameter with 
                // the length of an array out-parameter, so we have to process these first.
                if (checkNull()) {
                    writer.write("if (" + name + " != null) ");
                }
                writer.write(name + (isAliasForPrimitive() ? ".setValue(" : ".set("));
                String identifier = name + "POINTER.get(" + Conversions.getValueLayout(type) + ", 0)";
                if (isAliasForPrimitive() || (type.isPrimitive && type.isPointer())) {
                    writer.write(identifier);
                    if (type.isBoolean()) writer.write(" != 0");
                    writer.write(");\n");
                } else {
                    writer.write(marshalNativeToJava(type, identifier, false) + ");\n");
                }
            } else {
                // Secondly, process the array out parameters
                String len = array.size(false);
                String valuelayout = Conversions.getValueLayout(array.type);
                if (isOutParameter() && len != null) {
                    writer.write("if (" + name + " != null) " + name + ".set(");
                    // Out-parameter array
                    if (array.type.isPrimitive) {
                        writer.write(name + "POINTER.toArray(" + valuelayout + "));\n");
                    } else {
                        marshalNativeToJava(writer, name + "POINTER.address()", false);
                        writer.write(");\n");
                    }
                } else if (array.type.isPrimitive && (! array.type.isBoolean())) {
                    // Array of primitive values
                    writer.write("if (" + name + " != null) " + name + ".set(");
                    writer.write("MemorySegment.ofAddress(" + name + "POINTER.get(Interop.valueLayout.ADDRESS, 0), " + len + " * " + valuelayout + ".byteSize(), SCOPE).toArray(" + valuelayout + "));\n");
                } else {
                    // Array of proxy objects
                    writer.write("if (" + name + " != null) {\n");
                    writer.increaseIndent();
                    writer.write(array.type.qualifiedJavaType + "[] " + name + "ARRAY = new " + array.type.qualifiedJavaType + "[" + len + "];\n");
                    writer.write("for (int I = 0; I < " + len + "; I++) {\n");
                    writer.write("    var OBJ = " + name + "POINTER.get(" + valuelayout + ", I);\n");
                    writer.write("    " + name + "ARRAY[I] = ");
                    writer.write(marshalNativeToJava(array.type, "OBJ", false) + ";\n");
                    writer.write("    }\n");
                    writer.write(name + ".set(" + name + "ARRAY);\n");
                    writer.decreaseIndent();
                    writer.write("}\n");
                }
            }
        }
        
        // If the parameter has attribute transfer-ownership="full", we don't need to unref it anymore.
        // Only for proxy objects where ownership is fully transferred away, unless it's an out parameter or a pointer.
        if (isProxy()
                && "full".equals(transferOwnership) 
                && (! isOutParameter()) 
                && (type.cType == null || (! type.cType.endsWith("**")))) {
            String param = isInstanceParameter() ? "this" : name;
            if (nullable) writer.write("if (" + param + " != null) ");
            writer.write(param + ".yieldOwnership();\n");
        }
    }

    public void generateUpcallPreprocessing(SourceWriter writer) throws IOException {
        if (isAliasForPrimitive() && type.isPointer()) {
            String typeStr = Conversions.getValueLayout(type.girElementInstance.type);
            writer.write(type.qualifiedJavaType + " " + name + "ALIAS = new " + type.qualifiedJavaType + "(" + name + ".get(" + typeStr + ", 0));\n");
        } else if (isOutParameter()) {
            if (type != null) {
                String typeStr = type.qualifiedJavaType;
                if (type.isPrimitive) typeStr = Conversions.primitiveClassName(typeStr);
                writer.write("Out<" + typeStr + "> " + name + "OUT = new Out<>(");
                if (type.isPrimitive || type.isAliasForPrimitive()) {
                    String layout = Conversions.getValueLayout(type);
                    writer.write(name + ".get(" + layout + ", 0)");
                    if (type.isBoolean()) writer.write(" != 0");
                    writer.write(");\n");
                } else {
                    String identifier = name;
                    if (type.isEnum() || type.isBitfield()) {
                        identifier = name + ".get(Interop.valueLayout.C_INT, 0)";
                    }
                    writer.write(marshalNativeToJava(type, identifier, true) + ");\n");
                }
            }
            if (array != null) {
                writeType(writer, false);
                writer.write(" " + name + "OUT = new Out<>(");
                marshalNativeToJava(writer, name, true);
                writer.write(");\n");
            }
        }
    }

    public void generateUpcallPostprocessing(SourceWriter writer) throws IOException {
        if (type != null && type.isAliasForPrimitive() && type.isPointer()) {
            String typeStr = Conversions.getValueLayout(type.girElementInstance.type);
            writer.write(name + ".set(" + typeStr + ", 0, " + name + "ALIAS.getValue());\n");
        } else if (isOutParameter()) {
            if (type != null) {
                String typeStr = Conversions.getValueLayout(type);
                String identifier = marshalJavaToNative(type, name + "OUT.get()", true);
                if (type.isPrimitive || type.isAliasForPrimitive()) {
                    identifier = name + "OUT.get()";
                    if (type.isBoolean()) identifier += " ? 1 : 0";
                }
                if (type.isEnum() || type.isBitfield()) {
                    identifier = name + "OUT.get().getValue()";
                }
                writer.write(name + ".set(" + typeStr + ", 0, " + identifier + ");\n");
            }
            if (array != null) {
                // TODO: Copy the array from the Out<> parameter to the provided memory address.
            }
        }
    }
}
