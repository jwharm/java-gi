package io.github.jwharm.javagi.base;

import java.lang.foreign.Addressable;

/**
 * Represents an instance of a proxy object with a handle to an object 
 * in native memory.
 */
public interface Proxy {

    /**
     * Get the native memory address of the object
     * @return the native memory address
     */
    Addressable handle();
}
