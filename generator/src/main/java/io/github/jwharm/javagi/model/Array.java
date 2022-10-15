package io.github.jwharm.javagi.model;

public class Array extends GirElement {

    public final String cType, length, zeroTerminated, fixedSize;

    public Array(GirElement parent, String name, String cType, String length, String zeroTerminated, String fixedSize) {
        super(parent);
        this.name = name;
        this.cType = cType;
        this.length = length;
        this.zeroTerminated = zeroTerminated;
        this.fixedSize = fixedSize;
    }
    
    /**
     * Returns a String that will contain or retrieve the array size.
     */
    public String size() {
    	// fixed-size attribute: Return the value of the attribute
    	if (fixedSize != null) {
    		return fixedSize;
    	}
    	// the "length" attribute refers to another parameter, which contains the length
    	if (length != null) {
    		Parameter lp = ((Parameter) parent).getParameter(length);
    		if (lp != null) {
    			if (lp.type != null && (lp.type.isPointer() || lp.isOutParameter())) {
    				return lp.name + ".get().intValue()";
    			}
    			if (lp.type != null && lp.type.isAliasForPrimitive()) {
    				return lp.name + ".getValue()";
    			}
    			return lp.name;
    		}
    	}
    	// Size is unknown
    	return null;
    }
}
