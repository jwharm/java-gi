package io.github.jwharm.javagi;

import org.gtk.glib.GLib;
import org.jetbrains.annotations.ApiStatus;

import java.lang.foreign.Addressable;
import java.lang.ref.Cleaner;

/**
 * Abastract base class for proxy objects that represent a GObject instance 
 * in native memory. The {@link #handle()} method returns the memory address
 * of the object.
 */
public abstract class ObjectBase implements Proxy {

    private static final Cleaner cleaner = Cleaner.create();
    private final Addressable address;
    private RefCleaner refCleaner;
    private Cleaner.Cleanable cleanable;

    /**
     * Instantiate the ObjectBase class.
     * @param address the memory address of the object in native memory
     */
    @ApiStatus.Internal
    protected ObjectBase(Addressable address) {
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
     * Disable the Cleaner that automatically calls {@code g_object_unref} 
     * when this object is garbage collected.
     */
    public void yieldOwnership() {
        GLib.printerr("YieldOwnership %s %s\n", getClass().getSimpleName(), address.toString());
        if (this.refCleaner != null && this.refCleaner.registered) {
            this.refCleaner.registered = false;
        }
    }

    /**
     * Enable the Cleaner that automatically calls {@code g_object_unref}
     * when this object is garbage collected.
     */
    public void takeOwnership() {
        GLib.printerr("TakeOwnership %s %s\n", getClass().getSimpleName(), address.toString());

        if (this.refCleaner == null) {
            refCleaner = new RefCleaner(address);
            cleanable = cleaner.register(this, refCleaner);
        } else {
            this.refCleaner.registered = true;
        }
    }
}
