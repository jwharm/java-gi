package io.github.jwharm.javagi.base;

import io.github.jwharm.javagi.interop.MemoryCleaner;

import java.lang.foreign.MemorySegment;

/**
 * Base type for a Java proxy object to an instance in native memory.
 */
public class ProxyInstance implements Proxy {

    private final MemorySegment address;

    /**
     * Create a new {@code ProxyInstance} object for an instance in native memory.
     * @param address the memory address of the instance
     */
    public ProxyInstance(MemorySegment address) {
        this.address = address;
        MemoryCleaner.register(this);
    }

    /**
     * Get the memory address of the instance
     * @return the memory address of the instance
     */
    @Override
    public MemorySegment handle() {
        return address;
    }
}
