package io.github.jwharm.javagi.interop;

/**
 * Thrown when an unexpected error occurs when calling a native
 * function or reading/writing from/to native memory.
 */
public class InteropException extends RuntimeException {

    /**
     * Create an InteropException that wraps another Throwable
     * @param cause the Throwable to wrap in the InteropException
     */
    public InteropException(Throwable cause) {
        super(cause);
    }

    /**
     * Create an InteropException with the provided message.
     * @param message the exception message.
     */
    public InteropException(String message) {
        super(message);
    }
}
