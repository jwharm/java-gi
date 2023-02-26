package io.github.jwharm.javagi.base;

import io.github.jwharm.javagi.interop.InstanceCache;
import org.gnome.gobject.GObject;
import org.gnome.gobject.ToggleNotify;

import java.lang.foreign.Addressable;
import java.lang.ref.Cleaner;

/**
 * Runnable that is executed by a Cleaner to remove the toggle reference from an object.
 * This will cause the native GObject to be disposed.
 */
public class RefCleaner implements Runnable {

    // The address of the object
    private final Addressable address;
    private final ToggleNotify notify;

    /**
     * Create a new refCleaner instance that can be registered with a {@link Cleaner}.
     * @param address memory address of the object instance to be cleaned
     * @param notify the same ToggleNotify callback that was passed to {@link GObject#addToggleRef(ToggleNotify)}
     */
    public RefCleaner(Addressable address, ToggleNotify notify) {
        this.address = address;
        this.notify = notify;
    }

    /**
     * This function is run by the {@link Cleaner} when a {@link org.gnome.gobject.GObject}
     * instance has become unreachable, to remove the toggle reference.
     */
    public void run() {
        new GObject(address).removeToggleRef(notify);

        // Remove the empty mapping of address->weakReference(null) from the instance cache
        InstanceCache.weakReferences.remove(address);
    }
}
