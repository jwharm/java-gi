package io.github.jwharm.javagi;

import java.lang.foreign.Addressable;

/**
 * Base type for {@code struct} data that is not a GObject instance.
 */
public class Struct extends ObjectBase {

    /**
     * Create a new {@code Struct} object for a struct in native memory.
     * @param address    The memory address of the struct
     * @param ownership  The ownership indicator for the struct
     */
    public Struct(Addressable address, Ownership ownership) {
        super(address, ownership);
    }
}
