package io.github.jwharm.javagi.interop;

public class InteropException extends RuntimeException {

    public InteropException(Throwable cause) {
        super(cause);
    }

    public InteropException(String message) {
        super(message);
    }
}
