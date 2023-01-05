package io.github.jwharm.javagi;

import org.gtk.glib.GLib;

import java.lang.foreign.Addressable;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.ref.Cleaner;

/**
 * Runnable that is executed by a Cleaner to {@code unref} an object
 */
class RefCleaner implements Runnable {

    private Addressable address;
    boolean registered;

    // Method handle that is used for the g_object_unref native call
    private static MethodHandle g_object_unref;

    /**
     * Create a new refCleaner instance that can be registered with a {@link Cleaner}.
     * @param address memory address of the object instance to be cleaned
     */
    RefCleaner(Addressable address) {
        this.address = address;
        this.registered = true;
    }

    /**
     * This function is run by the {@link Cleaner} when an {@link ObjectBase} instance has become unreachable.
     * If the ownership is set, a call to {@code g_object_unref} is executed.
     */
    public void run() {
        if (registered) {
            try {
                GLib.printerr("Unref " + address.toString() + "\n");

                if (g_object_unref == null)
                    g_object_unref = Interop.downcallHandle(
                            "g_object_unref",
                            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                            false
                    );

                g_object_unref.invokeExact(address);

            } catch (Throwable err) {
                throw new AssertionError("Unexpected exception occured: ", err);
            }
        }
    }
}
