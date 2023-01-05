package io.github.jwharm.javagi;

import org.gtk.glib.GLib;
import org.jetbrains.annotations.ApiStatus;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.ref.Cleaner;

/**
 * Abastract base class for proxy objects that represent a GObject instance 
 * in native memory. The {@link #handle()} method returns the memory address
 * of the object.
 * For ref-counted objects where ownership is transferred to the user, a 
 * Cleaner is registered by the constructor that will automatically call 
 * {@code g_object_unref} when the proxy object is garbage-collected.
 */
public abstract class ObjectBase implements Proxy {

    private final Addressable address;
    private static final Cleaner cleaner = Cleaner.create();
    private State state;
    private Cleaner.Cleanable cleanable;

    // Method handle that is used for the g_object_unref native call
    private static MethodHandle g_object_unref;

    // The State class is used by the Cleaner
    private static class State implements Runnable {
        Addressable address;
        boolean registered;

        State(Addressable address) {
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
                    GLib.print("Unref %s\n", address.toString());
                    // Debug logging
                    // System.out.println("g_object_unref " + address);

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
        GLib.print("YieldOwnership %s %s\n", getClass().getSimpleName(), address.toString());
        if (this.state != null && this.state.registered) {
            this.state.registered = false;
        }
    }

    /**
     * Enable the Cleaner that automatically calls {@code g_object_unref}
     * when this object is garbage collected.
     */
    public void takeOwnership() {
        GLib.print("TakeOwnership %s %s\n", getClass().getSimpleName(), address.toString());
        if (this.state == null) {
            state = new State(address);
            cleanable = cleaner.register(this, state);
        } else {
            this.state.registered = true;
        }
    }
}
