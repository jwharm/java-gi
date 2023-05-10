package io.github.jwharm.javagi.interop;

import io.github.jwharm.javagi.base.Proxy;
import org.gnome.glib.GLib;
import org.gnome.gobject.TypeClass;
import org.gnome.gobject.TypeInstance;
import org.gnome.gobject.TypeInterface;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.ref.Cleaner;
import java.util.HashMap;
import java.util.Map;

/**
 * This class keeps a cache of all memory addresses for which a Proxy object
 * was created (except for GObject instances; those are handled in the InstanceCache).
 * <p>
 * When a new Proxy object is created, the refererence count in the cache is increased.
 * When a Proxy object is garbage-collected, the reference count in the cache is decreased.
 * When the reference count is 0, the memory is released using {@link GLib#free(MemorySegment)}
 * or a specialized method.
 * <p>
 * When ownership of a memory address is passed to native code, the cleaner will not free
 * the memory. Ownership is enabled/disabled with {@link #takeOwnership(MemorySegment)} and
 * {@link #yieldOwnership(MemorySegment)}.
 */
public class MemoryCleaner {

    private static final Cleaner CLEANER = Cleaner.create();

    private static final Map<MemorySegment, Cached> references = new HashMap<>();

    /**
     * Register the memory address of this proxy to be cleaned when the proxy gets garbage-collected.
     * @param proxy The Proxy object
     */
    public static void register(Proxy proxy) {
        // Do not cache GObjects, they are cached in the InstanceCache
        if (proxy instanceof TypeInstance || proxy instanceof TypeClass || proxy instanceof TypeInterface) {
            return;
        }

        // Put the address in the cache (or increase the refcount)
        MemorySegment address = proxy.handle();
        Cached cached = references.get(address);
        if (cached == null) {
            references.put(address, new Cached(false, 1, null));
            CLEANER.register(proxy, new StructFinalizer(address));
        } else {
            references.put(address, new Cached(false, cached.references + 1, cached.freeFunc));
        }
    }

    /**
     * Register a specialized cleanup function for this memory address, instead of
     * the default {@link GLib#free(MemorySegment)}.
     * @param address the memory address
     * @param freeFunc the specialized cleanup function to call
     */
    public static void setFreeFunc(MemorySegment address, String freeFunc) {
        Cached cached = references.get(address);
        references.put(address, new Cached(cached.owned, cached.references, freeFunc));
    }

    /**
     * Take ownership of this memory address: when all proxy objects are garbage-collected,
     * the memory will automatically be released.
     * @param address the memory address
     */
    public static void takeOwnership(MemorySegment address) {
        Cached cached = references.get(address);
        references.put(address, new Cached(true, cached.references, cached.freeFunc));
    }

    /**
     * Yield ownership of this memory address: when all proxy objects are garbage-collected,
     * the memory will not be released.
     * @param address the memory address
     */
    public static void yieldOwnership(MemorySegment address) {
        Cached cached = references.get(address);
        references.put(address, new Cached(false, cached.references, cached.freeFunc));
    }

    /**
     * This record type is cached for each memory address.
     * @param owned whether this address is owned (should be cleaned)
     * @param references the numnber of references (active Proxy objects) for this address
     * @param freeFunc an (optional) specialized function that will release the native memory
     */
    private record Cached(boolean owned, int references, String freeFunc) {}

    /**
     * This callback is run by the {@link Cleaner} when a struct or union instance
     * has become unreachable, to free the native memory.
     */
    private static class StructFinalizer implements Runnable {

        private final MemorySegment address;

        /**
         * Create a new StructFinalizer that will be run by the {@link Cleaner}
         * @param address the native memory address
         */
        public StructFinalizer(MemorySegment address) {
            this.address = address;
        }

        /**
         * This method is run by the {@link Cleaner} when the last Proxy object for this
         * memory address is garbage-collected.
         */
        public void run() {
            Cached cached = references.get(address);
            if (cached.references > 1) {
                // Other references exist: Decrease refcount
                references.put(address, new Cached(cached.owned, cached.references - 1, cached.freeFunc));
            } else {
                // No other references exist: Remove address from the cache and (if ownership is enabled) run free()
                references.remove(address);
                if (cached.owned) {
                    if (cached.freeFunc != null) {
                        try {
                            Interop.downcallHandle(
                                    cached.freeFunc,
                                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                                    false
                            ).invokeExact(address);
                        } catch (Throwable e) {
                            throw new AssertionError("Unexpected exception occured: ", e);
                        }
                    } else {
                        GLib.free(address);
                    }
                }
            }
        }
    }
}
