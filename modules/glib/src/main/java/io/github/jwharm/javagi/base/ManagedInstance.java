package io.github.jwharm.javagi.base;

import io.github.jwharm.javagi.interop.MemoryCleaner;

import java.lang.foreign.MemorySegment;

public class ManagedInstance extends ProxyInstance {

    /**
     * Create a new {@code ManagedInstance} object for an instance in native memory,
     * and register it in the {@link MemoryCleaner} so it will be automatically cleaned
     * during garbage collection.
     *
     * @param address the memory address of the instance
     */
    public ManagedInstance(MemorySegment address) {
        super(address);
        MemoryCleaner.register(this);
    }
}
