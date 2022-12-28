package io.github.jwharm.javagi.model;

public interface CallableType {

    Parameters getParameters();
    void setParameters(Parameters ps);

    ReturnValue getReturnValue();
    void setReturnValue(ReturnValue rv);

    /** Performs a series of checks to determine if this call can be mapped to C. */
    default boolean isSafeToBind() {
        Parameters ps = getParameters();
        if (ps != null) {
            boolean isSignal = this instanceof Signal;
            for (Parameter p : ps.parameterList) {
                // We don't support out parameter arrays with unknown length
                if (p.isOutParameter() && p.array != null && p.array.size(false) == null) return false;

                // Check for signals with out parameters or arrays
                if (isSignal) {
                    if (p.isOutParameter()) return false;
                    if (p.array != null) return false;
                }
            }
        }

        ReturnValue rv = getReturnValue();
        if (rv.type != null) {
            // We don't support callback return values yet
            if (rv.type.isCallback()) return false;
        }

        return true;
    }
}
