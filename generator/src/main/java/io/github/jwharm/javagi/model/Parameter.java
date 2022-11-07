package io.github.jwharm.javagi.model;

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
     * If the transfer-ownership attribute is not set, Ownership.UNKNOWN is retuned
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
}
