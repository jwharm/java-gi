package io.github.jwharm.javagi;

import java.lang.foreign.Addressable;

/**
 * Base type for a Java proxy object to a {@code struct} in native memory.
 */
public class Struct implements Proxy {

    private final Addressable address;

    /**
     * Create a new {@code Struct} object for a struct in native memory.
     * @param address    the memory address of the struct
     */
    public Struct(Addressable address) {
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

    /**
     * No op
     */
    @Override
    public void yieldOwnership() {
    }

    /**
     * No op
     */
    @Override
    public void takeOwnership() {
    }

    /**
     * No op
     */
    @Override
    public void setRefCleanerMethod(String method) {
    }
}
