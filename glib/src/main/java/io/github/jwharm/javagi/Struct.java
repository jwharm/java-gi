package io.github.jwharm.javagi;

import java.lang.foreign.Addressable;

/**
 * Base type for {@code struct} data that is not a GObject instance.
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
    
    @Override
    public Addressable handle() {
        return address;
    }
    
    @Override
    public Ownership yieldOwnership() {
        this.ownership = Ownership.NONE;
        return ownership;
    }
}
