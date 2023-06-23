package io.github.jwharm.javagi.base;

/**
 * This exception is thrown when a function is run on a platform where it is not available.
 */
public class UnsupportedPlatformException extends RuntimeException {

    /**
     * Create a new UnsupportedPlatformException with the provided message
     * @param message the exception message
     */
    public UnsupportedPlatformException(String message) {
        super();
    }
}
