package io.github.jwharm.javagi.base;

/**
 * Classes that implement the Floating interface, have a refSink method
 */
public interface Floating extends Proxy {

    Floating refSink();

    void unref();
}
