package io.github.jwharm.javagi.base;

import java.lang.foreign.Addressable;
import java.lang.ref.Cleaner;

/**
 * Abastract base class for proxy objects that represent a GObject instance 
 * in native memory. The {@link #handle()} method returns the memory address
 * of the object.
 */
public abstract class ObjectProxy implements Proxy {

    private static final Cleaner cleaner = Cleaner.create();

    private final Addressable address;
    private RefCleaner refCleaner;
    private Cleaner.Cleanable cleanable;

    /**
     * Instantiate the ObjectProxy class.
     * @param address the memory address of the object in native memory
     */
    protected ObjectProxy(Addressable address) {
        this.address = address;
    }

    /**
     * Return the memory address of the object in native memory
     * @return The native memory address of the object
     */
    public Addressable handle() {
        return this.address;
    }
    
    /**
     * Disable the Cleaner that automatically calls {@code g_object_unref} (or
     * another method that has been specified) when this object is garbage collected.
     */
    public void yieldOwnership() {
        if (refCleaner != null && refCleaner.registered) {
            refCleaner.registered = false;
        }
    }

    /**
     * Enable the Cleaner that automatically calls {@code g_object_unref} (or
     * another method that has been specified) when this object is garbage collected.
     */
    public void takeOwnership() {
        if (this.refCleaner == null) {
            refCleaner = new RefCleaner(address);
            cleanable = cleaner.register(this, refCleaner);
        } else {
            refCleaner.registered = true;
        }
    }

    /**
     * Change the method name from the default {@code g_object_unref} to
     * another method name.
     * @param method the unref method name
     */
    public void setRefCleanerMethod(String method) {
        if (refCleaner != null) {
            refCleaner.refCleanerMethod = method;
            refCleaner.unrefMethodHandle = null;
        }
    }
}
