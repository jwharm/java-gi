package io.github.jwharm.javagi.model;

public interface CallableType {

    Parameters getParameters();
    void setParameters(Parameters ps);

    ReturnValue getReturnValue();
    void setReturnValue(ReturnValue rv);

    /** Performs a series of checks to determine if this call can be mapped to C. */
    default boolean isSafeToBind() {
        Parameters ps = getParameters();
        ReturnValue rv = getReturnValue();

        if (ps != null) {

            if (ps.parameterList.stream().anyMatch(p ->

                       // We don't support out parameter arrays with unknown length
                       (p.isOutParameter() && p.array != null && p.array.size() == null)
            )) {
                return false;
            }

            // We don't support methods with a callback parameter but no user_data parameter
            if (ps.parameterList.stream().anyMatch(Parameter::isCallbackParameter)
                    && ps.parameterList.stream().noneMatch(Parameter::isUserDataParameter)
            ) {
                return false;
            }
        }
        
        // Check for signals with out parameters or arrays
        if (this instanceof Signal && ps != null) {
            if (ps.parameterList.stream().anyMatch(Parameter::isOutParameter)) {
                return false;
            }
            if (ps.parameterList.stream().anyMatch(p -> p.array != null)) {
                return false;
            }
        }

        if (rv.type == null) return true;

        // We don't support callbacks yet
        if (rv.type.isCallback()) return false;

        return true;
    }
}
