package io.github.jwharm.javagi;

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

    /**
     * Disable the Cleaner that automatically calls {@code unref}
     * when this object is garbage collected.
     */
    void yieldOwnership();

    /**
     * Enable the Cleaner that automatically calls {@code unref}
     * when this object is garbage collected.
     */
    void takeOwnership();

    /**
     * Change the method name from the default {@code g_object_unref} to
     * another method name.
     * @param method the unref method name
     */
    void setRefCleanerMethod(String method);
    
    /**
     * Check if the memory address of this proxy instance is equal to the 
     * memory address of the provided proxy instance.
     * @param proxy another Proxy instance
     * @return true when the memory addresses are equal, false otherwise
     */
    default boolean equals(Proxy proxy) {
        return handle() != null && handle().equals(proxy.handle());
    }
}
