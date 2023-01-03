package io.github.jwharm.javagi;

import java.lang.foreign.Addressable;

/**
 * Base type for a Java proxy object to a {@code struct} in native memory.
 */
public class Struct implements Proxy {

    private final Addressable address;
    private Ownership ownership;
    
    /**
     * Create a new {@code Struct} object for a struct in native memory.
     * @param address    the memory address of the struct
     * @param ownership  the ownership indicator for the struct
     */
    public Struct(Addressable address, Ownership ownership) {
        this.address = address;
        this.ownership = ownership;
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
     * Set the Ownership indicator to {@link Ownership#NONE}.
     * @return the new ownership indicator of this object, always {@link Ownership#NONE}.
     */
    @Override
    public Ownership yieldOwnership() {
        this.ownership = Ownership.NONE;
        return ownership;
    }
}
