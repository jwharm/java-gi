package io.github.jwharm.javagi.base;

/**
 * Classes that implement the Floating interface, have a refSink method
 */
public interface Floating extends Proxy {

    /**
     * Sink the floating reference
     * @return the instance
     */
    Floating refSink();

    /**
     * Decrease the reference count of the instance
     */
    void unref();
}
