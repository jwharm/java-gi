package io.github.jwharm.javagi.base;

/**
 * This exception is thrown when a function is run on a platform where it is
 * not available (i.e. not in the gobject-introspection repository for this platform).
 */
public class UnsupportedPlatformException extends Exception {

    public UnsupportedPlatformException(String message) {
        super();
    }
}
