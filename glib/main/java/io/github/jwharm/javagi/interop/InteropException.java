package io.github.jwharm.javagi.interop;

import java.io.Serial;

/**
 * {@code InteropException} is a {@link RuntimeException} thrown when an unexpected
 * error occurs during native memory access.
 */
public class InteropException extends RuntimeException {

    // Auto-generated
    @Serial
    private static final long serialVersionUID = 6054712690701505684L;

    /**
     * Create a new InteropException
     */
    public InteropException() {
    }

    /**
     * Create a new InteropException with the provided message
     * @param message the exception message
     */
    public InteropException(String message) {
        super(message);
    }

    /**
     * Create a new InteropException with the provided message and cause
     * @param message the exception message
     * @param cause the wrapped exception
     */
    public InteropException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new InteropException that wraps the provided exception
     * @param cause the wrapped exception
     */
    public InteropException(Throwable cause) {
        super(cause);
    }
}
