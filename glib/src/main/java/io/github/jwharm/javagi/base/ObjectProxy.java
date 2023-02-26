package io.github.jwharm.javagi.base;

import org.gnome.gobject.TypeInstance;

import java.lang.foreign.Addressable;

/**
 * Abstract base class for proxy objects that represent an object instance
 * in native memory.
 */
public abstract class ObjectProxy extends TypeInstance {

    /**
     * Instantiate the ObjectProxy class.
     * @param address the memory address of the object in native memory
     */
    protected ObjectProxy(Addressable address) {
        super(address);
    }
}
