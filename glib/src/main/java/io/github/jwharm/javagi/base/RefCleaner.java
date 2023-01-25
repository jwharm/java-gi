package io.github.jwharm.javagi.base;

import io.github.jwharm.javagi.interop.Interop;

import java.lang.foreign.Addressable;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.ref.Cleaner;

/**
 * Runnable that is executed by a Cleaner to {@code unref} an object
 */
class RefCleaner implements Runnable {

    // The address of the object
    private final Addressable address;

    // The method name that unrefs the object
    String refCleanerMethod = "g_object_unref";

    // Used to enable/disable the cleaner
    boolean registered;

    // Method handle that is used for the native call
    MethodHandle unrefMethodHandle;

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
     * If the ownership is set, a call to {@code g_object_unref} (or another method that has been setup) is executed.
     */
    public void run() {
        if (registered) {
            try {

                if (unrefMethodHandle == null)
                    unrefMethodHandle = Interop.downcallHandle(
                            refCleanerMethod,
                            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                            false
                    );

                unrefMethodHandle.invokeExact(address);

            } catch (Throwable err) {
                throw new AssertionError("Unexpected exception occured: ", err);
            }
        }
    }
}
