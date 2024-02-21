/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.interop;

import io.github.jwharm.javagi.base.Proxy;
import org.gnome.glib.GLib;
import org.gnome.glib.Type;

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
 * When a new Proxy object is created, the reference count in the cache is increased.
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
        MemorySegment address = proxy.handle();
        synchronized (references) {
            Cached cached = references.get(address);
            if (cached == null) {
                // Put the address in the cache
                var cleanable = CLEANER.register(proxy, new StructFinalizer(address));
                references.put(address, new Cached(false, 1, null,
                        null, cleanable));
            } else {
                // Already in the cache: increase the refcount
                references.put(address, new Cached(false, cached.references + 1, cached.freeFunc,
                        cached.boxedType, cached.cleanable));
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
            if (cached != null)
                references.put(address, new Cached(cached.owned, cached.references, freeFunc,
                        cached.boxedType, cached.cleanable));
        }
    }

    /**
     * For a boxed type, {@code g_boxed_free(type, pointer)} will be used as cleanup function.
     * @param address the memory address
     * @param boxedType the boxed type
     */
    public static void setBoxedType(MemorySegment address, Type boxedType) {
        synchronized (references) {
            Cached cached = references.get(address);
            if (cached != null)
                references.put(address, new Cached(cached.owned, cached.references, cached.freeFunc,
                        boxedType, cached.cleanable));
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
            if (cached != null)
                references.put(address, new Cached(true, cached.references, cached.freeFunc,
                        cached.boxedType, cached.cleanable));
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
            if (cached != null)
                references.put(address, new Cached(false, cached.references, cached.freeFunc,
                        cached.boxedType, cached.cleanable));
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
     * @param references the number of references (active Proxy objects) for this address
     * @param freeFunc an (optional) specialized function that will release the native memory
     */
    private record Cached(boolean owned, int references, String freeFunc, Type boxedType, Cleaner.Cleanable cleanable) {}

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
                    references.put(address, new Cached(cached.owned, cached.references - 1,
                            cached.freeFunc, cached.boxedType, cached.cleanable));
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

            try {
                if (cached.boxedType != null) {
                    // free boxed type
                    Interop.downcallHandle(
                            "g_boxed_free",
                            FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                            false
                    ).invokeExact(cached.boxedType.getValue(), address);
                } else {
                    // Run specialized free function
                    Interop.downcallHandle(
                            cached.freeFunc,
                            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                            false
                    ).invokeExact(address);
                }
            } catch (Throwable e) {
                throw new AssertionError("Unexpected exception occurred: ", e);
            }
        }
    }
}
