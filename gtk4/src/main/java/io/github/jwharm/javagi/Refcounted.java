package io.github.jwharm.javagi;

import java.lang.foreign.Addressable;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.ref.Cleaner;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class Refcounted {

    private final static Cleaner cleaner = Cleaner.create();
    private Refcounted.State state;
    private Cleaner.Cleanable cleanable;

    // Method handle that is used for the g_object_unref native call
    private static final MethodHandle g_object_unref = Interop.downcallHandle(
            "g_object_unref",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
            false
    );

    // The State class is used by the Cleaner
    private static class State implements Runnable {
        Addressable address;
        Ownership ownership;

        State(Addressable address, Ownership owned) {
            this.address = address;
            this.ownership = owned;
        }

        public void run() {
            if (ownership == Ownership.FULL) {
                try {
                    g_object_unref.invokeExact(address);
                } catch (Throwable ERR) {
                    throw new AssertionError("Unexpected exception occured: ", ERR);
                }
            }
        }
    }

    // Private constructor. Use Refcounted.get() to obtain a Refcounted instance.
    private Refcounted(Addressable handle, Ownership owned) {
        state = new Refcounted.State(handle, owned);
        cleanable = cleaner.register(this, state);
    }

    /**
     * Returns the memory address of the ref-counted object
     * @return the memory address
     */
    public final Addressable handle() {
        return state.address;
    }

    /**
     * Set ownership of the ref-counted object. For objects where 
     * {@code owned == FULL}, the {@code unref()} function is automatically 
     * called during garbage collection.
     * @param ownership whether the ownership of the ref-counted object belongs to the user
     */
    public void setOwnership(Ownership ownership) {
        state.ownership = ownership;
    }
    
    /**
     * Get ownership of the ref-counted object.
     * @return The ownership indicator
     */
    public Ownership getOwnership() {
        return state.ownership;
    }

    /**
     * Set the ownership attribute of this Refcounted object to NONE,
     * so it will <strong>not</strong> be {@code unref()}-ed when cleaned.
     * @return the Refcounted instance
     */
    public Refcounted unowned() {
        setOwnership(Ownership.NONE);
        return this;
    }

    /**
     * Set the ownership attribute of this Refcounted object to FULL,
     * so it will be {@code unref()}-ed when cleaned.
     * @return the Refcounted instance
     */
    public Refcounted owned() {
        setOwnership(Ownership.FULL);
        return this;
    }

    // We cache and reuse (memoize) all Refcounted objects, based on the memory address.
    // This means that when different native functions return a reference to the same 
    // object, we re-use the same Refcounted instance in Java.
    // The cache is a WeakHashMap, which means that the cache keeps a weak reference 
    // to the Refcounted instances. When they become unreachable from the application, 
    // they are garbage collected (even though they are still in the cache). After the 
    // GC has run, the weak reference disappears from the cache.
    private static final Set<Refcounted> cache = Collections.newSetFromMap(
            new WeakHashMap<Refcounted, Boolean>()
    );

    /**
     * Retrieve the Refcounted instance for this memory address from the cache,
     * or add a new instance to the cache if it did not yet exist. Ownership
     * of an existing Refcounted instance remains unchanged. For new instances,
     * ownership is set to NONE.
     * 
     * @param address The memory address of a refcounted object to lookup in the cache
     * @return A Refcounted object, or null when address is null (or MemoryAddress.NULL)
     */
    public static Refcounted get(Addressable address) {
    	if (address == null || address.equals(MemoryAddress.NULL)) {
    		return null;
    	}
        for (Refcounted r : cache) {
            if (r.handle().equals(address)) {
                return r;
            }
        }
        Refcounted ref = new Refcounted(address, Ownership.NONE);
        cache.add(ref);
        return ref;
    }

    /**
     * Retrieve the Refcounted for this memory address from the cache,
     * or add a new instance to the cache if it did not yet exist.
     * Ownership is updated to the given value unless Ownership.UNCHANGED 
     *           is provided, in which case the ownership is unchanged.
     *           When the address did not exist in the cache and the 
     *           requested ownership was UNCHANGED, the ownership for the 
     *           new Refcounted object is NONE.
     * 
     * @param address The memory address of the object
     * @param ownership Ownership indicator
     * @return The Refcounted instance from the cache, or a new Refcounted
     *         instance if it did not yet exist
     */
    public static Refcounted get(Addressable address, Ownership ownership) {
        Refcounted ref = get(address);
        if (ownership != Ownership.UNKNOWN) {
            ref.setOwnership(ownership);
        }
        return ref;
    }
}
