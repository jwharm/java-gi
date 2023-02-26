package io.github.jwharm.javagi.base;

import java.lang.foreign.Addressable;

/**
 * Base type for a Java proxy object to a {@code struct} in native memory.
 */
public class StructProxy implements Proxy {

    private final Addressable address;

    /**
     * Create a new {@code Struct} object for a struct in native memory.
     * @param address    the memory address of the struct
     */
    public StructProxy(Addressable address) {
        this.address = address;
    }

    /**
     * Get the memory address of the struct
     * @return the memory address of the struct
     */
    @Override
    public Addressable handle() {
        return address;
    }
}
