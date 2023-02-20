package io.github.jwharm.javagi.base;

import org.gnome.gobject.TypeInstance;

import java.lang.foreign.Addressable;
import java.lang.ref.Cleaner;

/**
 * Abstract base class for proxy objects that represent an object instance
 * in native memory. For ref-counted objects, a Cleaner is attached that
 * will unref the instance when the proxy instance has ownership and is
 * garbage-collected.
 */
public abstract class ObjectProxy extends TypeInstance {

    private static final Cleaner CLEANER = Cleaner.create();
    private RefCleaner refCleaner;

    /**
     * Instantiate the ObjectProxy class.
     * @param address the memory address of the object in native memory
     */
    protected ObjectProxy(Addressable address) {
        super(address);
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
            refCleaner = new RefCleaner(handle());
            CLEANER.register(this, refCleaner);
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
