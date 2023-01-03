package io.github.jwharm.javagi;

/**
 * Describes ownership of a function argument or return value.
 */
public enum Ownership {

    /**
     * Ownership is transferred.
     * For an argument, ownership is transferred from the caller to the callee.
     * For a return value, ownership is transferred from the callee to the caller.
     */
    FULL,

    /**
     * Ownership of the values in the container is transferred.
     * For an argument, ownership is transferred from the caller to the callee.
     * For a return value, ownership is transferred from the callee to the caller.
     */
    CONTAINER,

    /**
     * The value has a floating reference and is initially unowned.
     */
    FLOATING,

    /**
     * Ownership is not transferred.
     */
    NONE,

    /**
     * Unknown whether ownership is transferred.
     */
    UNKNOWN
}
