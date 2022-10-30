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
    
    public boolean transferOwnership() {
        return "full".equals(transferOwnership);
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

    public void generateTypeAndName(Writer writer, boolean pointerForArray) throws IOException {
        // Annotations
        if (type != null && !type.isPrimitive) writer.write(nullable ? "@Nullable " : "@NotNull ");

        if (array != null) {
            // Out parameters
            if (isOutParameter()) {
            	writer.write("Out<" + array.type.qualifiedJavaType + "[]>");
            } else if (pointerForArray) {
                writer.write(getPointerReturnType(array.type, null));
            } else {
                writer.write(array.type.qualifiedJavaType + "[]");
            }
        
        // Out parameters
        } else if (isOutParameter()) {
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

}
