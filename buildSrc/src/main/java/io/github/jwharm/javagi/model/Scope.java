package io.github.jwharm.javagi.model;

import java.util.Arrays;

/**
 * Scope (lifetime) of callbacks
 */
public enum Scope {

    /**
     * (Default.) Only valid for the duration of the call. Can be called multiple times during the call.
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
     * No scope attribute set
     */
    NONE;

    /**
     * Create a Scope from the provided String, case-insensitive. Defaults to {@link #CALL}.
     *
     * @param value a String to convert to a Scope.
     * @return the Scope
     */
    public static Scope from(String value) {
        if (value == null || value.isEmpty()) {
            return NONE;
        }
        try {
            return Scope.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
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
}
