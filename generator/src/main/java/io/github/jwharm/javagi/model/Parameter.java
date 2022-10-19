package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

import io.github.jwharm.javagi.generator.Conversions;

public class Parameter extends GirElement {

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
    public Parameter getParameter(String indexAttr) {
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
    
    public boolean transferOwnership() {
        return "full".equals(transferOwnership);
    }
    
    public boolean isOutParameter() {
    	return direction != null && direction.contains("out");
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

    public void generateTypeAndName(Writer writer, boolean pointerForArray) throws IOException {
        // Annotations
        if (type != null && !type.isPrimitive) writer.write(nullable ? "@Nullable " : "@NotNull ");

        if (array != null) {
            // Arrays
            generateArrayType(writer, array.type, pointerForArray);

        } else if (isOutParameter()) {
            // Out parameters
        	String typeStr = type.qualifiedJavaType;
        	if (type.isPrimitive) {
        		typeStr = Conversions.primitiveClassName(type.simpleJavaType);
        	}
    		writer.write("Out<" + typeStr + ">");

        } else if (type.cType != null && type.cType.endsWith("**")) {
            // Also arrays
            writer.write(getPointerReturnType(type, null));

        } else if (type.isPrimitive && type.isPointer()) {
            // Pointer to primitive type
            writer.write("Pointer" + Conversions.primitiveClassName(type.simpleJavaType));

        } else {
            // Everything else
            writer.write(type.qualifiedJavaType);
        }
        writer.write(" " + name);
    }
    
    private void generateArrayType(Writer writer, Type type, boolean pointerForArray) throws IOException {
        // Out parameters
        if (isOutParameter()) {
        	writer.write("Out<" + type.qualifiedJavaType + "[]>");
        } else if (pointerForArray) {
            writer.write(getPointerReturnType(type, null));
        } else {
            writer.write(type.qualifiedJavaType + "[]");
        }
    }

    public void generateInterop(Writer writer) throws IOException {
        if (array != null) {
            // Arrays
            generateArrayInterop(writer, array.type);

        } else if (type == null) {
            // This should not happen
            writer.write(name);

        } else if (isOutParameter()) {
            // Out parameters
            if (nullable) writer.write(name + " == null ? MemoryAddress.NULL : ");
        	writer.write("(Addressable) " + name + "POINTER.address()");

        } else if (type.cType != null && type.cType.endsWith("**")) {
            // Pointers
            if (nullable) writer.write(name + " == null ? MemoryAddress.NULL : ");
            writer.write(name + ".handle()");

        } else if (type.qualifiedJavaType.equals("java.lang.String")) {
            // Strings: allocate utf8 string
            if (nullable) writer.write(name + " == null ? MemoryAddress.NULL : ");
            writer.write("Interop.allocateNativeString(" + name + ")");

        } else if (type.isPrimitive && type.isPointer()) {
            // Pointer to primitive type: get memory address
            if (nullable) writer.write(name + " == null ? MemoryAddress.NULL : ");
            writer.write(name + ".handle()");

        } else if (type.isBoolean()) {
            // Convert boolean to int
            writer.write(name + " ? 1 : 0");

        } else if (type.girElementInstance != null) {
            // Objects and ValueWrappers
            if (nullable) writer.write(name + " == null ? MemoryAddress.NULL : ");
            writer.write(type.girElementInstance.getInteropString(name, type.isPointer(), transferOwnership()));

        } else {
            // Primitive types
            writer.write(name);
        }
    }
    
    private void generateArrayInterop(Writer writer, Type type) throws IOException {
        if (nullable) writer.write(name + " == null ? MemoryAddress.NULL : ");
        if (type == null) {
            // This should not happen
            writer.write("MemoryAddress.NULL");
        
        } else if (isOutParameter()) {
            writer.write("(Addressable) " + name + "POINTER.address()");

        } else if (type.isEnum() || type.isBitfield() || type.isAliasForPrimitive()) {
            // Convert array of ValueWrapper types to an array of the wrapped values
            String typename = "";
            if (type.isAliasForPrimitive()) {
                typename = Conversions.primitiveClassName(type.girElementInstance.type.qualifiedJavaType);
            }
            writer.write("Interop.allocateNativeArray(" + type.qualifiedJavaType + ".get" + typename + "Values(" + name + "))");

        } else {
            // Automatically use the right allocateNativeArray() method for this type
            writer.write("Interop.allocateNativeArray(" + name + ")");
        }
    }
    
    public String getNewInstanceString(Type type, String identifier) {
    	if (type == null) {
    		return identifier;
    	}
    	if (array == null && type.cType != null && type.cType.endsWith("**") && (! isOutParameter())) {
    		return "new PointerProxy<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + ".class)";
    	}
    	if (type.qualifiedJavaType.equals("java.lang.String")) {
    		return identifier + ".getUtf8String(0)";
    	}
    	if (type.isBitfield() || type.isEnum() || type.isAliasForPrimitive()) {
    		return "new " + type.qualifiedJavaType + "(" + identifier + ")";
    	}
    	if (type.isCallback()) {
    		return "null /* Unsupported parameter type */";
    	}
    	if (type.isBoolean()) {
    		return identifier + " != 0";
    	}
    	if (type.isInterface()) {
    		return "new " + type.qualifiedJavaType + "." + type.simpleJavaType + "Impl(Refcounted.get(" + identifier + ", " + (transferOwnership() ? "true" : "false") + "))";
    	}
    	if (type.isClass() || type.isAlias() || type.isUnion()) {
    		return "new " + type.qualifiedJavaType + "(Refcounted.get(" + identifier + ", " + (transferOwnership() ? "true" : "false") + "))";
    	}
    	return identifier;
    }

    public void generateReverseInterop(Writer writer, String identifier) throws IOException {
        if (nullable) writer.write(identifier + " == null ? null : ");

        if (array != null) {
            // Arrays
            generateReverseArrayInterop(writer, identifier);

        } else if (type.cType != null && type.cType.endsWith("**")) {
            // Also arrays, but in this case it's always a pointer to an object
            writer.write("new PointerProxy<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + ".class)");

        } else if (type.qualifiedJavaType.equals("java.lang.String")) {
            // Create Java String from UTF8 memorysegment
            writer.write(identifier + ".getUtf8String(0)");

        } else if (type.isPrimitive && type.isPointer()) {
            // Create Pointer object
            writer.write("new Pointer" + Conversions.primitiveClassName(type.simpleJavaType) + "(" + identifier + ")");

        } else if (type.isBitfield() || type.isEnum() || type.isAliasForPrimitive()) {
            // Create ValueWrapper object
            writer.write("new " + type.qualifiedJavaType + "(" + identifier + ")");

        } else if (type.isCallback()) {
            // I don't think this situation exists
            writer.write("null /* Unsupported parameter type */");

        } else if (type.isBoolean()) {
            // Convert int back to boolean
            writer.write(identifier + " != 0");

        } else if (type.isPrimitive) {
            // Primitive values remain as-is
            writer.write(identifier);

        } else if (type.isInterface()) {
            // Create an Impl object when we only know the interface but not the class
            writer.write("new " + type.qualifiedJavaType + "." + type.simpleJavaType + "Impl(Refcounted.get(" + identifier + ", " + (transferOwnership() ? "true" : "false") + "))");

        } else if (type.isClass() || type.isAlias() || type.isUnion()) {
            // Objects
            writer.write("new " + type.qualifiedJavaType + "(Refcounted.get(" + identifier + ", " + (transferOwnership() ? "true" : "false") + "))");

        } else {
            // Anything else
            writer.write(identifier);
        }
    }
    
    public void generateReverseArrayInterop(Writer writer, String identifier) throws IOException {
        if (nullable) writer.write(identifier + " == null ? null : ");

        Type type = array != null ? array.type : this.type;
    	String len = null;
    	if (array != null) {
    		len = array.size();
    	}

        if (array != null && array.array != null) {
            // Array of arrays - this is not supported yet
            writer.write("");

        } else if (type == null) {
            // This should not happen
            writer.write("null");
        
        } else if (len != null) {
			String valuelayout = Conversions.getValueLayout(type);
			writer.write("MemorySegment.ofAddress(" + identifier + ".get(ValueLayout.ADDRESS, 0), " + len + " * " + valuelayout + ".byteSize(), Interop.getScope()).toArray(" + valuelayout + ")");

        } else if (type.isEnum()) {
            // Pointer to enumeration
            writer.write("new PointerEnumeration<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + ".class)");

        } else if (type.isBitfield()) {
            // Pointer to bitfield
            writer.write("new PointerBitfield<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + ".class)");

        } else if (type.isAliasForPrimitive()) {
            // Pointer to wrapped primitive value
            writer.write("new Pointer" + Conversions.primitiveClassName(type.girElementInstance.type.qualifiedJavaType) + "(" + identifier + ")");

        } else if (type.isPrimitive) {
            // Pointer to primitive value
            writer.write("new Pointer" + Conversions.primitiveClassName(type.qualifiedJavaType) + "(" + identifier + ")");

        } else if (type.qualifiedJavaType.equals("java.lang.String")) {
            // Pointer to UTF8 memorysegment
            writer.write("new PointerString(" + identifier + ")");

        } else if (type.qualifiedJavaType.equals("java.lang.foreign.MemoryAddress")) {
            // Pointer to memorysegment
            writer.write("new PointerAddress(" + identifier + ")");

        } else {
            // Pointer to object
            writer.write("new PointerProxy<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + ".class)");
        }
    }
    
    public String getReturnType() {
        // Arrays
        if (array != null) {
        	return getPointerReturnType(array.type, array.size());
        }
        // Also arrays, but in this case it's always a pointer to an object
        if (type.cType != null && type.cType.endsWith("**")) {
        	return "PointerProxy<" + type.qualifiedJavaType + ">";
        }
        // Create Pointer object
        if (type.isPrimitive && type.isPointer()) {
            return "Pointer" + Conversions.primitiveClassName(type.simpleJavaType);
        }
        // Anything else
        return type.qualifiedJavaType;
    }
    
    public String getPointerReturnType(Type type, String size) {
        // This should not happen
        if (type == null) {
            return "void";
        }
        // Size is known?
        if (size != null) {
        	return type.qualifiedJavaType + "[]";
        }
        // Pointer to enumeration
        if (type.isEnum()) {
            return "PointerEnumeration";
        }
        // Pointer to bitfield
        if (type.isEnum()) {
            return "PointerBitfield";
        }
        // Pointer to wrapped primitive value
        if (type.isAliasForPrimitive()) {
            return "Pointer" + Conversions.primitiveClassName(type.girElementInstance.type.qualifiedJavaType);
        }    
        // Pointer to primitive value
        if (type.isPrimitive) {
            return "Pointer" + Conversions.primitiveClassName(type.qualifiedJavaType);
        }
        // Pointer to UTF8 memorysegment
        if (type.qualifiedJavaType.equals("java.lang.String")) {
            return "PointerString";
        }
        // Pointer to pointer
        if (type.qualifiedJavaType.equals("java.lang.foreign.MemoryAddress")) {
            return "PointerAddress";
        }    
        // Pointer to object
        return "PointerProxy<" + type.qualifiedJavaType + ">";
    }
}
