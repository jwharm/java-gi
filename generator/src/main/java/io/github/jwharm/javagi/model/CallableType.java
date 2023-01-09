package io.github.jwharm.javagi.model;

public interface CallableType {

    Parameters getParameters();
    void setParameters(Parameters ps);

    ReturnValue getReturnValue();
    void setReturnValue(ReturnValue rv);

    Doc getDoc();

    String getThrows();

    /**
     * This function is used to determine if memory is allocated to marshal
     * the call parameters or return value.
     * @return true when memory is allocated (for an array, a native string,
     *         an out parameter, or a pointer to a primitive value)
     */
    default boolean allocatesMemory() {
        if (getParameters() != null) {
            if (getParameters().parameterList.stream().anyMatch(
                    p -> (p.array != null)
                            || (p.type != null && "java.lang.String".equals(p.type.qualifiedJavaType))
                            || (p.isOutParameter())
                            || (p.isAliasForPrimitive() && p.type.isPointer())
            )) {
                return true;
            }
        }
        ReturnValue rv = getReturnValue();
        return getThrows() != null
                || rv.array != null
                || (this instanceof Closure && rv.type != null && "java.lang.String".equals(rv.type.qualifiedJavaType));
    }
}
