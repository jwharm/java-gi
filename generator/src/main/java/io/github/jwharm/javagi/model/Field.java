package io.github.jwharm.javagi.model;

import java.io.IOException;
import java.io.Writer;

import io.github.jwharm.javagi.generator.Conversions;

public class Field extends Variable {

    public final String readable, isPrivate;
    public Callback callback;

    public Field(GirElement parent, String name, String readable, String isPrivate) {
        super(parent);
        this.name = name;
        this.readable = readable;
        this.isPrivate = isPrivate;
    }
    
    public void generate(Writer writer) throws IOException {
    	// Don't try to generate a getter for callback or array fields yet
    	if (type == null) {
    		return;
    	}
    	// Don't generate a getter for a "disguised" record (often private data)
    	if (type.girElementInstance != null 
    			&& type.girElementInstance instanceof Record r 
    			&& "1".equals(r.disguised)) {
    		return;
    	}
    	// Don't generate a getter for a field that is marked as not readable
    	if ("0".equals(readable)) {
    		return;
    	}
    	// Don't generate a getter for a private field
    	if ("1".equals(isPrivate)) {
    		return;
    	}
    	writer.write("    \n");
    	writer.write("    /**\n");
    	writer.write("     * @return The value of the field {@code " + this.name + "}\n");
    	writer.write("     */\n");
    	writer.write("    public " + getReturnType() + " " + this.name + "$get() {\n");
    	
    	if ((! type.isPointer()) && (type.isClass() || type.isInterface())) {
    		writer.write("        long OFFSET = getMemoryLayout().byteOffset(MemoryLayout.PathElement.groupElement(\"" + this.name + "\"));\n");
            writer.write("        return ");
            generateReverseInterop(writer, "((MemoryAddress) handle()).addOffset(OFFSET)", false);
            writer.write(";\n");
    	} else {
        	writer.write("        var RESULT = (" + getMemoryType() + ") getMemoryLayout()\n"
        			+ "            .varHandle(MemoryLayout.PathElement.groupElement(\"" + this.name + "\"))\n"
        			+ "            .get(MemorySegment.ofAddress((MemoryAddress) handle(), getMemoryLayout().byteSize(), Interop.getScope()));\n");
            writer.write("        return ");
            generateReverseInterop(writer, "RESULT", false);
            writer.write(";\n");
    	}
    	writer.write("    }\n");
    }
    
    public String getMemoryLayoutString() {
    	if (type != null) {
    		if (type.isBitfield() || type.isEnum()) {
    			return "Interop.valueLayout.C_INT.withName(\"" + this.name + "\")";
    		}
    		if (type.isPointer()
    				|| "java.lang.String".equals(type.qualifiedJavaType)
					|| "java.lang.foreign.MemoryAddress".equals(type.qualifiedJavaType)
					|| type.isCallback()) {
        		return "Interop.valueLayout.ADDRESS.withName(\"" + this.name + "\")";
    		}
    		if (type.isPrimitive 
    				|| type.isAliasForPrimitive()) {
        		return Conversions.getValueLayout(type) + ".withName(\"" + this.name + "\")";
    		}
    		return type.qualifiedJavaType + ".getMemoryLayout()" + ".withName(\"" + this.name + "\")";
    	}
    	if (array != null && array.fixedSize != null) {
    		String valueLayout = Conversions.toPanamaMemoryLayout(array.type);
        	return "MemoryLayout.sequenceLayout(" + array.fixedSize + ", " + valueLayout + ").withName(\"" + this.name + "\")";
    	}
    	if (array != null) {
    		return "Interop.valueLayout.ADDRESS.withName(\"" + this.name + "\")";
    	}
    	if (callback != null) {
    		return "Interop.valueLayout.ADDRESS.withName(\"" + this.name + "\")";
    	}
		System.out.printf("Error: Field %s.%s has unknown type\n", parent.name, name);
		return "Interop.valueLayout.ADDRESS.withName(\"" + this.name + "\")";
    }
    
    /**
     * Get the native type of this field. For example "int", "char".
     * An array with fixed size is returned as "ARRAY", a pointer is returned as "MemoryAddress".
     * @return the native type of this field
     */
    public String getMemoryType() {
    	if (type != null) {
    		return getMemoryType(type);
    	} else if (array != null && array.fixedSize != null) {
    		return "ARRAY";
    	} else {
			return "MemoryAddress";
    	}
    }
    
    /**
     * Get the native type of a non-array type
     * @param type the type
     * @return the native type
     */
    public String getMemoryType(Type type) {
    	if (type.isPrimitive && type.isPointer()) {
    		return "MemoryAddress";
    	}
		if (type.isBoolean() || type.isBitfield() || type.isEnum()) {
			return "int";
		}
		if (type.isPrimitive) {
			return type.simpleJavaType;
		}
		if (type.isAliasForPrimitive()) {
			return type.girElementInstance.type.simpleJavaType;
		}
		return "MemoryAddress";
    }
    
    /**
     * Get the byte-size of the native types. This is obviously architecture- and platform-specific.
     * @param memoryType The result of getMemoryType(). For example: int, byte, ...
     * @return the native byte-size of the provided type
     */
    public int getSize(String memoryType) {
    	return switch(memoryType) {
    		case "boolean" -> 32; // treated as an integer
    		case "byte" -> 8;
    		case "char" -> 8;
    		case "short" -> 16;
    		case "int" -> 32;
    		case "long" -> 64; // On Windows this is 32, on Linux this is 64
    		case "float" -> 32;
    		case "double" -> 64;
    		case "MemoryAddress" -> 64; // 64-bits pointer
    		case "ARRAY" -> Integer.valueOf(array.fixedSize) * getSize(getMemoryType(array.type));
    		default -> 64;
    	};
    }
}
