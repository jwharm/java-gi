package io.github.jwharm.javagi;

import java.lang.foreign.Addressable;

import org.jetbrains.annotations.ApiStatus;

/**
 * Represents an instance of a proxy object with a handle to an object 
 * in native memory.
 */
public interface Proxy {

    /**
     * Get the native memory address of the object
     * @return the native memory address
     */
    @ApiStatus.Internal
    Addressable handle();

    /**
     * Disable the Cleaner that automatically calls {@code g_object_unref} 
     * when this object is garbage collected.
     */
    @ApiStatus.Internal
    void yieldOwnership();

    /**
     * Enable the Cleaner that automatically calls {@code g_object_unref}
     * when this object is garbage collected.
     */
    @ApiStatus.Internal
    void takeOwnership();
    
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
