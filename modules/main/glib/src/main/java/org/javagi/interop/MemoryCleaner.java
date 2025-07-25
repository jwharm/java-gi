/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 Jan-Willem Harmannij
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package org.javagi.interop;

import org.javagi.base.Proxy;
import org.gnome.glib.GLib;
import org.gnome.glib.Type;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.ref.Cleaner;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * This class keeps a cache of all memory addresses for which a Proxy object
 * was created (except for GObject instances; those are handled in the
 * InstanceCache).
 * <p>
 * When a cached (and owned) Proxy object is garbage-collected, the native
 * memory is released using {@code g_boxed_free}, a custom free-function, or
 * (as a last resort), {@code g_free}.
 * <p>
 * When ownership of a memory address transfers to native code, the cleaner
 * will not free the memory. Take and yield ownership with
 * {@link #takeOwnership} and {@link #yieldOwnership}.
 */
public class MemoryCleaner {

    private static final Cleaner CLEANER = Cleaner.create();
    private static final Map<MemorySegment, Cached> cache = new HashMap<>();

    /**
     * Register the memory address of this proxy to be cleaned when the proxy
     * gets garbage-collected.
     *
     * @param proxy The proxy instance
     */
    private static @NotNull Cached getOrRegister(@NotNull Proxy proxy) {
        MemorySegment address = proxy.handle();
        synchronized (cache) {
            Cached cached = cache.get(address);
            if (cached == null) {
                // Put the address in the cache
                var finalizer = new StructFinalizer(address);
                var cleanable = CLEANER.register(proxy, finalizer);
                cached = new Cached(false, null, null, cleanable);
                cache.put(address, cached);
            }
            return cached;
        }
    }

    /**
     * Register a specialized cleanup function for this proxy instance, instead
     * of the default {@link GLib#free(MemorySegment)}.
     *
     * @param proxy    the proxy instance
     * @param freeFunc the specialized cleanup function to call
     */
    public static void setFreeFunc(@NotNull Proxy proxy,
                                   @NotNull String freeFunc) {
        requireNonNull(proxy);
        requireNonNull(freeFunc);
        synchronized (cache) {
            Cached cached = getOrRegister(proxy);
            cache.put(proxy.handle(), new Cached(cached.owned,
                                                 freeFunc,
                                                 cached.boxedType,
                                                 cached.cleanable));
        }
    }

    /**
     * For a boxed type, {@code g_boxed_free(type, pointer)} will be used as
     * cleanup function.
     *
     * @param proxy     the proxy instance
     * @param boxedType the boxed type
     */
    public static void setBoxedType(@NotNull Proxy proxy,
                                    @NotNull Type boxedType) {
        requireNonNull(proxy);
        requireNonNull(boxedType);
        synchronized (cache) {
            Cached cached = getOrRegister(proxy);
            cache.put(proxy.handle(), new Cached(cached.owned,
                                                 cached.freeFunc,
                                                 boxedType,
                                                 cached.cleanable));
        }
    }

    /**
     * Take ownership of this memory address: when the proxy object is
     * garbage-collected, the memory will automatically be released.
     *
     * @param proxy  the proxy instance
     */
    public static void takeOwnership(@NotNull Proxy proxy) {
        requireNonNull(proxy);

        // Don't try to take ownership of a null pointer
        if (MemorySegment.NULL.equals(proxy.handle()))
            return;

        synchronized (cache) {
            Cached cached = getOrRegister(proxy);
            cache.put(proxy.handle(), new Cached(true,
                                                 cached.freeFunc,
                                                 cached.boxedType,
                                                 cached.cleanable));
        }
    }

    /**
     * Yield ownership of this memory address: when the proxy object is
     * garbage-collected, the memory will not be released.
     *
     * @param proxy  the proxy instance
     */
    public static void yieldOwnership(@NotNull Proxy proxy) {
        requireNonNull(proxy);
        synchronized (cache) {
            Cached cached = cache.get(proxy.handle());
            if (cached != null) {
                cache.put(proxy.handle(), new Cached(false,
                                                     cached.freeFunc,
                                                     cached.boxedType,
                                                     cached.cleanable));
                cached.cleanable.clean();
            }
        }
    }

    /**
     * Run the {@link StructFinalizer} associated with this memory address, by
     * invoking {@link Cleaner.Cleanable#clean()}.
     * <p>
     * The cleaner action will only ever run once, so any further attempts to
     * free this instance (including by the GC) will be a no-op.
     *
     * @param address the memory address to free
     */
    public static void free(MemorySegment address) {
        synchronized (cache) {
            Cached cached = cache.get(address);
            if (cached != null)
                cached.cleanable.clean();
        }
    }

    /**
     * This record type is cached for each memory address.
     *
     * @param owned     whether this address is owned (should be cleaned)
     * @param freeFunc  an (optional) specialized function that will release
     *                  the native memory
     * @param boxedType an (optional) GType of a boxed type to release with
     *                  {@code g_boxed_free}
     * @param cleanable a cleaning action that will be run by the GC
     */
    private record Cached(boolean owned,
                          String freeFunc,
                          Type boxedType,
                          Cleaner.Cleanable cleanable) {
    }

    /**
     * This callback is run by the {@link Cleaner} when a Java Proxy object for
     * a native struct/union has become unreachable, to free the native memory.
     */
    private record StructFinalizer(MemorySegment address) implements Runnable {

        private static final MethodHandle g_boxed_free = Interop.downcallHandle(
                "g_boxed_free",
                FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG,
                                          ValueLayout.ADDRESS),
                false
        );

        /**
         * This method is run by the {@link Cleaner} when the last Proxy object
         * for this memory address is garbage-collected.
         */
        public void run() {
            Cached cached;
            synchronized (cache) {
                // Retrieve and remove the address from the cache
                cached = cache.get(address);
                cache.remove(address);
            }

            // if we don't have ownership, we must not run the free-function
            if (!cached.owned) {
                return;
            }

            // run the free-function
            try {
                if (cached.boxedType != null) {
                    // free boxed type
                    long gtype = cached.boxedType.getValue();
                    g_boxed_free.invokeExact(gtype, address);
                } else if (cached.freeFunc != null) {
                    // Run specialized free-function
                    Interop.downcallHandle(
                            cached.freeFunc,
                            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                            false
                    ).invokeExact(address);
                } else {
                    // Fallback to g_free()
                    GLib.free(address);
                }
            } catch (Throwable err) {
                throw new AssertionError(err);
            }
        }
    }
}
