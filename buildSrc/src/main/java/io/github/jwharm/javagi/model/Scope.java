package io.github.jwharm.javagi.model;

import java.util.Arrays;

/**
 * Scope (lifetime) of callbacks
 */
public enum Scope {

    /**
     * Only valid for the duration of the call. Can be called multiple times during the call.
     * (try-with-resources scope)
     */
    CALL,

    /**
     * Only valid for the duration of the first callback invocation. Can only be called once. (try-with-resources
     * scope)
     */
    ASYNC,

    /**
     * Valid until the GDestroyNotify argument is called. Can be called multiple times before the GDestroyNotify is
     * called.
     */
    NOTIFIED,

    /**
     * Valid until the process terminates. (global scope)
     */
    FOREVER,

    /**
     * Scope bound to instance lifetime (default for instance methods)
     */
    BOUND;

    /**
     * Create a Scope from the provided String, case-insensitive. Defaults to {@link #CALL}.
     *
     * @param value a String to convert to a Scope.
     * @return the Scope
     */
    public static Scope from(String value) {
        if (value == null || value.isEmpty()) {
            return BOUND;
        }
        try {
            return Scope.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BOUND;
        }
    }

    /**
     * Return true if the provided list contains this Scope
     *
     * @param scopes the list to check
     * @return true if the list contains this Scope
     */
    public boolean in(Scope... scopes) {
        return Arrays.asList(scopes).contains(this);
    }

    /**
     * Get the Scope of a variable:
     * <ul>
     * <li>Field: Always {@link #CALL}
     * <li>Parameter: The value of the {@code "scope"} attribute if present, else {@link #BOUND}, except for
     * constructor and function parameters: in that case {@link #FOREVER}.
     * </ul>
     *
     * @param variable the variable to get the scope for
     * @return the scope as described above
     */
    public static Scope ofVariable(Variable variable) {
        var scope = variable instanceof Parameter p ? p.scope
                : variable instanceof Field ? Scope.CALL
                : null;

        if (scope == Scope.BOUND) {
            var method = variable.parent.parent;
            if (method instanceof Constructor || method instanceof Function)
                scope = Scope.FOREVER;
        }
        return scope;
    }
}
