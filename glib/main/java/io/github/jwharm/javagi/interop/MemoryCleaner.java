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

    private static final Map<MemorySegment, Cached> references = new HashMap<>();
    private static final Cleaner CLEANER = Cleaner.create();

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
        synchronized (references) {
            Cached cached = references.get(address);
            if (cached == null) {
                var cleanable = CLEANER.register(proxy, new StructFinalizer(address));
                references.put(address, new Cached(false, 1, null, cleanable));
            } else {
                references.put(address, new Cached(false, cached.references + 1, cached.freeFunc, cached.cleanable));
            }
        }
    }

    /**
     * Register a specialized cleanup function for this memory address, instead of
     * the default {@link GLib#free(MemorySegment)}.
     * @param address the memory address
     * @param freeFunc the specialized cleanup function to call
     */
    public static void setFreeFunc(MemorySegment address, String freeFunc) {
        synchronized (references) {
            Cached cached = references.get(address);
            references.put(address, new Cached(cached.owned, cached.references, freeFunc, cached.cleanable));
        }
    }

    /**
     * Take ownership of this memory address: when all proxy objects are garbage-collected,
     * the memory will automatically be released.
     * @param address the memory address
     */
    public static void takeOwnership(MemorySegment address) {
        synchronized (references) {
            Cached cached = references.get(address);
            references.put(address, new Cached(true, cached.references, cached.freeFunc, cached.cleanable));
        }
    }

    /**
     * Yield ownership of this memory address: when all proxy objects are garbage-collected,
     * the memory will not be released.
     * @param address the memory address
     */
    public static void yieldOwnership(MemorySegment address) {
        synchronized (references) {
            Cached cached = references.get(address);
            references.put(address, new Cached(false, cached.references, cached.freeFunc, cached.cleanable));
        }
    }

    /**
     * Run the {@link StructFinalizer} associated with this memory address, by invoking
     * {@link Cleaner.Cleanable#clean()}.
     * @param address the memory address to free
     */
    public static void free(MemorySegment address) {
        synchronized (references) {
            Cached cached = references.get(address);
            cached.cleanable.clean();
        }
    }

    /**
     * This record type is cached for each memory address.
     * @param owned whether this address is owned (should be cleaned)
     * @param references the numnber of references (active Proxy objects) for this address
     * @param freeFunc an (optional) specialized function that will release the native memory
     */
    private record Cached(boolean owned, int references, String freeFunc, Cleaner.Cleanable cleanable) {}

    /**
     * This callback is run by the {@link Cleaner} when a struct or union instance has become unreachable, to free the
     * native memory.
     */
    private record StructFinalizer(MemorySegment address) implements Runnable {

        /**
         * This method is run by the {@link Cleaner} when the last Proxy object for this memory address is
         * garbage-collected.
         */
        public void run() {
            Cached cached;
            synchronized (references) {
                cached = references.get(address);

                // When other references exist, decrease the refcount
                if (cached.references > 1) {
                    references.put(address, new Cached(cached.owned, cached.references - 1, cached.freeFunc, cached.cleanable));
                    return;
                }

                // When no other references exist, remove the address from the cache and free the memory
                references.remove(address);
            }

            // if we don't have ownership, we must not run free()
            if (!cached.owned) {
                return;
            }

            // run g_free
            if (cached.freeFunc == null) {
                GLib.free(address);
                return;
            }

            // Run specialized free function
            try {
                Interop.downcallHandle(
                        cached.freeFunc,
                        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                        false
                ).invokeExact(address);
            } catch (Throwable e) {
                throw new AssertionError("Unexpected exception occured: ", e);
            }
        }
    }
}
