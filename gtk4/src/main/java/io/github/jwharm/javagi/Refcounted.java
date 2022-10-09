package io.github.jwharm.javagi;

import java.lang.foreign.Addressable;
import java.lang.foreign.FunctionDescriptor;
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

    private static final MethodHandle g_object_unref = Interop.downcallHandle(
            "g_object_unref",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
    );

    private static class State implements Runnable {
        Addressable address;
        boolean owned;

        State(Addressable address, boolean owned) {
            this.address = address;
            this.owned = owned;
        }

        public void run() {
            if (owned) {
                try {
                    g_object_unref.invokeExact(address);
                } catch (Throwable ERR) {
                    throw new AssertionError("Unexpected exception occured: ", ERR);
                }
            }
        }
    }

    public Refcounted(Addressable handle, boolean owned) {
        state = new Refcounted.State(handle, owned);
        cleanable = cleaner.register(this, state);
    }

    public final Addressable handle() {
        return state.address;
    }

    public void setOwnership(boolean owned) {
        state.owned = owned;
    }

    public Refcounted unowned() {
        setOwnership(false);
        return this;
    }

    public Refcounted owned() {
        setOwnership(true);
        return this;
    }

    private static final Set<Refcounted> cache = Collections.newSetFromMap(
            new WeakHashMap<Refcounted, Boolean>()
    );

    /**
     * Retrieve the Refcounted instance for this memory address from the cache,
     * or add a new instance to the cache if it did not yet exist. Ownership
     * of an existing Refcounted instance remains unchanged. For new instances,
     * ownership is set to false.
     */
    public static Refcounted get(Addressable address) {
        for (Refcounted r : cache) {
            if (r.handle().equals(address)) {
                return r;
            }
        }
        Refcounted ref = new Refcounted(address, false);
        cache.add(ref);
        return ref;
    }

    /**
     * Retrieve the Refcounted for this memory address from the cache,
     * or add a new instance to the cache if it did not yet exist.
     * Ownership is updated to the given value.
     */
    public static Refcounted get(Addressable address, boolean owned) {
        Refcounted ref = get(address);
        ref.setOwnership(owned);
        return ref;
    }
}
