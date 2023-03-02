package io.github.jwharm.javagi.base;

public interface Floating extends Proxy {

    Floating refSink();

    void unref();
}
