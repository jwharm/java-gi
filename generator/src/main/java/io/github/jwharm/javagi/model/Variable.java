package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

import io.github.jwharm.javagi.generator.Conversions;

public class Variable extends GirElement {

	public Variable(GirElement parent) {
		super(parent);
	}

    public void generateTypeAndName(Writer writer, boolean pointerForArray) throws IOException {
        // Annotations
        if (type != null && (! type.isPrimitive) && (this instanceof Parameter p)) {
        	writer.write(p.nullable ? "@Nullable " : "@NotNull ");
        }

        if (array != null) {
            // Out parameters
            if (this instanceof Parameter p && p.isOutParameter()) {
            	writer.write("Out<" + array.type.qualifiedJavaType + "[]>");
            } else if (pointerForArray) {
                writer.write(getPointerReturnType(array.type, null));
            } else {
                writer.write(array.type.qualifiedJavaType + "[]");
            }
        
        // Out parameters
        } else if (this instanceof Parameter p && p.isOutParameter()) {
        	String typeStr = pointerForArray ? getReturnType() : type.qualifiedJavaType;
        	if (type.isPrimitive) {
        		typeStr = Conversions.primitiveClassName(type.simpleJavaType);
        	}
    		writer.write("Out<" + typeStr + ">");
        
        // Also arrays
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

    public void generateInterop(Writer writer, String identifier, boolean checkForOutParameter) throws IOException {
        // Arrays
        if (array != null) {
            generateArrayInterop(writer, identifier, array.type, array.zeroTerminated);
        
        // This should not happen
        } else if (type == null) {
            writer.write(identifier);
        
        // Out parameters
        } else if (checkForOutParameter && this instanceof Parameter p && p.isOutParameter()) {
        	writer.write("(Addressable) " + identifier + "POINTER.address()");
        
        // Pointers
        } else if (type.cType != null && type.cType.endsWith("**")) {
            writer.write(identifier + ".handle()");
        
        // Strings: allocate utf8 string
        } else if (type.qualifiedJavaType.equals("java.lang.String")) {
            writer.write("Interop.allocateNativeString(" + identifier + ")");
        
        // Pointer to primitive type: get memory address
        } else if (type.isPrimitive && type.isPointer()) {
            writer.write(identifier + ".handle()");
        
        // Convert boolean to int
        } else if (type.isBoolean()) {
            writer.write(identifier + " ? 1 : 0");
        
        // Objects and ValueWrappers
        } else if (type.girElementInstance != null) {
        	boolean transferOwnership = this instanceof Parameter p && p.transferOwnership();
            writer.write(type.girElementInstance.getInteropString(identifier, type.isPointer(), transferOwnership));
        
        // Primitive types
        } else {
            writer.write(identifier);
        }
    }
    
    private void generateArrayInterop(Writer writer, String identifier, Type type, String zeroTerminated) throws IOException {
    	String zeroTerminatedBool = "1".equals(zeroTerminated) ? "true" : "false";
    	
        // This should not happen
        if (type == null) {
            // This should not happen
            writer.write("MemoryAddress.NULL");
        
        } else if (this instanceof Parameter p && p.isOutParameter()) {
            writer.write("(Addressable) " + identifier + "POINTER.address()");

        } else if (type.isEnum() || type.isBitfield() || type.isAliasForPrimitive()) {
            // Convert array of ValueWrapper types to an array of the wrapped values
            String typename = "";
            if (type.isAliasForPrimitive()) {
                typename = Conversions.primitiveClassName(type.girElementInstance.type.qualifiedJavaType);
            }
            writer.write("Interop.allocateNativeArray(" + type.qualifiedJavaType + ".get" + typename + "Values(" + identifier + "), " + zeroTerminatedBool + ")");

        } else {
            writer.write("Interop.allocateNativeArray(" + identifier + ", " + zeroTerminatedBool + ")");
        }
    }
    
    public String getNewInstanceString(Type type, String identifier, boolean generatePointerProxy) {
        // This should not happen
    	if (type == null) {
    		return identifier;
    	}
    	// Create Pointer to an object
    	if (array == null && type.cType != null && type.cType.equals("gchar**") && generatePointerProxy) {
    		return "new PointerString(" + identifier + ")";
    	}
    	// Create Pointer to an object
    	if (array == null && type.cType != null && type.cType.endsWith("**") && generatePointerProxy) {
    		return "new PointerProxy<" + type.qualifiedJavaType + ">(" + identifier + ", " + type.qualifiedJavaType + ".class)";
    	}
        // Create Pointer to primitive value
    	if (type.isPrimitive && type.isPointer()) {
        	return "new Pointer" + Conversions.primitiveClassName(type.simpleJavaType) + "(" + identifier + ")";
    	}
        // Create Java String from UTF8 memorysegment
    	if (type.qualifiedJavaType.equals("java.lang.String")) {
    		return "Interop.getStringFrom(" + identifier + ")";
    	}
        // Create ValueWrapper object
    	if (type.isBitfield() || type.isEnum() || type.isAliasForPrimitive()) {
    		return "new " + type.qualifiedJavaType + "(" + identifier + ")";
    	}
        // I don't think this situation exists
    	if (type.isCallback()) {
    		return "null /* Unsupported parameter type */";
    	}
        // Convert int back to boolean
    	if (type.isBoolean()) {
    		return identifier + " != 0";
    	}
        // Create an Impl object when we only know the interface but not the class
    	if (type.isInterface()) {
        	boolean transferOwnership = this instanceof Parameter p && p.transferOwnership();
    		return "new " + type.qualifiedJavaType + "." + type.simpleJavaType + "Impl(Refcounted.get(" + identifier + ", " + (transferOwnership ? "true" : "false") + "))";
    	}
        // Objects
    	if (type.isClass() || type.isAlias() || type.isUnion()) {
        	boolean transferOwnership = this instanceof Parameter p && p.transferOwnership();
    		return "new " + type.qualifiedJavaType + "(Refcounted.get(" + identifier + ", " + (transferOwnership ? "true" : "false") + "))";
    	}
        // Primitive values remain as-is
    	return identifier;
    }

    public void generateReverseInterop(Writer writer, String identifier, boolean pointerForArrays) throws IOException {
        if (array != null) {
            generateReverseArrayInterop(writer, identifier, pointerForArrays);
        } else {
        	writer.write(getNewInstanceString(type, identifier, true));
        }
    }
    
    public void generateReverseArrayInterop(Writer writer, String identifier, boolean pointerForArrays) throws IOException {
    	Type type = array != null ? array.type : this.type;
    	String len = null;
    	if (array != null) {
    		len = array.size();
    	}
    	
        // Array of arrays - this is not supported yet
        if (array != null && array.array != null) {
            // Array of arrays - this is not supported yet
            writer.write("null /* Return type not supported yet */");

        } else if (type == null) {
            // This should not happen
            writer.write("null");
        
        } else if ((! pointerForArrays) && len != null) {
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
            // Array of String arrays
        	if (array.array != null && "gchar***".equals(array.cType)) {
        		return "java.lang.String[][]";
        	}
            return getPointerReturnType(array.type, array.size());
        }
        // Pointers
        if (type.cType != null && type.cType.endsWith("**")) {
        	return getPointerReturnType(type, null);
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
