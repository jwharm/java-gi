package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

public class Field extends GirElement {

    public final String readable, isPrivate;
    public Callback callback;

    public Field(GirElement parent, String name, String readable, String isPrivate) {
        super(parent);
        this.name = name;
        this.readable = readable;
        this.isPrivate = isPrivate;
    }
    
    public String getMemoryLayoutString() {
    	if (type != null) {
    		if (type.isPrimitive 
    				|| type.isAliasForPrimitive()) {
        		return Conversions.getValueLayout(type) + ".withName(\"" + this.name + "\")";
			} else if ("java.lang.String".equals(type.qualifiedJavaType)
					|| "java.lang.foreign.MemoryAddress".equals(type.qualifiedJavaType)
					|| type.isCallback()) {
        		return "Interop.valueLayout.ADDRESS.withName(\"" + this.name + "\")";
    		} else {
        		return type.qualifiedJavaType + ".getMemoryLayout()" + ".withName(\"" + this.name + "\")";
    		}
    	} else if (array != null && array.fixedSize != null) {
    		String valueLayout = Conversions.toPanamaMemoryLayout(array.type);
        	return "MemoryLayout.sequenceLayout(" + array.fixedSize + ", " + valueLayout + ").withName(\"" + this.name + "\")";
    	} else if (array != null) {
    		return "Interop.valueLayout.ADDRESS.withName(\"" + this.name + "\")";
    	} else if (callback != null) {
    		return "Interop.valueLayout.ADDRESS.withName(\"" + this.name + "\")";
    	} else {
			System.out.printf("Error: Field %s.%s has unknown type\n", parent.name, name);
    		return "Interop.valueLayout.ADDRESS.withName(\"" + this.name + "\")";
    	}
    }
    
    public String getMemoryType() {
    	if (type != null) {
    		return getMemoryType(type);
    	} else if (array != null && array.fixedSize != null) {
    		return "ARRAY";
    	} else {
			return "ADDRESS";
    	}
    }
    
    public String getMemoryType(Type type) {
		if (type.isPrimitive) {
			return type.simpleJavaType;
		} else if (type.isAliasForPrimitive()) {
			return type.girElementInstance.type.simpleJavaType;
		} else if (type.isBitfield() || type.isEnum()) {
			return "int";
		} else {
			return "ADDRESS";
		}
    }
    
    public int getSize(String memoryType) {
    	return switch(memoryType) {
    		case "boolean" -> 32;
    		case "byte" -> 8;
    		case "char" -> 8;
    		case "short" -> 16;
    		case "int" -> 32;
    		case "long" -> 64;
    		case "float" -> 32;
    		case "double" -> 64;
    		case "address" -> 64;
    		case "array" -> Integer.valueOf(array.fixedSize) * getSize(getMemoryType(array.type));
    		default -> 64;
    	};
    }
}
