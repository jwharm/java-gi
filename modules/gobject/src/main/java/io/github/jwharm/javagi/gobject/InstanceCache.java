/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.gobject;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.github.jwharm.javagi.base.Floating;
import io.github.jwharm.javagi.base.GLibLogger;
import io.github.jwharm.javagi.gobject.types.TypeCache;
import io.github.jwharm.javagi.gobject.types.Types;
import io.github.jwharm.javagi.interop.Interop;
import org.gnome.glib.GLib;
import org.gnome.glib.MainContext;
import org.gnome.glib.Type;
import org.gnome.gobject.*;

import io.github.jwharm.javagi.base.Proxy;

/**
 * Caches Proxy instances so the same instance is used for the same memory
 * address.
 */
public class InstanceCache {

    private sealed interface Ref permits Ref.Strong, Ref.Weak {

        record Strong(Proxy proxy) implements Ref {
            public Proxy get() {
                return proxy;
            }
        }

        record Weak(WeakReference<Proxy> proxy) implements Ref {
            Weak(Proxy proxy) {
                this(new WeakReference<>(proxy));
            }
            public Proxy get() {
                return proxy == null ? null : proxy.get();
            }
        }

        Proxy get();

        default Ref asWeak() {
            return this instanceof Weak ? this : new Weak(get());
        }

        default Ref asStrong() {
            return this instanceof Strong ? this : new Strong(get());
        }
    }

    private static final ConcurrentHashMap<MemorySegment, Ref> references
            = new ConcurrentHashMap<>();

    private static final Cleaner CLEANER = Cleaner.create();

    private static final MethodHandle g_object_add_toggle_ref =
            Interop.downcallHandle(
                    "g_object_add_toggle_ref",
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    false);

    private static final MethodHandle g_object_remove_toggle_ref =
            Interop.downcallHandle(
                    "g_object_remove_toggle_ref",
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    false);

    private static final MethodHandle g_object_unref =
            Interop.downcallHandle(
                    "g_object_unref",
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                    false);

    private static final MemorySegment toggle_notify;

    private static final Type GOBJECT = GObject.getType();

    static {
        GObjects.javagi$ensureInitialized();

        // Create an upcall stub for the "handleToggleNotify" function
        try {
            FunctionDescriptor fdesc = FunctionDescriptor.ofVoid(
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT);
            var handle = MethodHandles.lookup().findStatic(
                    InstanceCache.class,
                    "handleToggleNotify",
                    fdesc.toMethodType()
            );
            toggle_notify = Linker.nativeLinker()
                    .upcallStub(handle, fdesc, Arena.global());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Internal helper function to retrieve a Proxy object from the
     * Strong/WeakReferences caches.
     *
     * @param  address get the Proxy object for this address from the cache
     * @return the instance (if found), or null (if not found)
     */
    private static Proxy lookup(MemorySegment address) {
        
        // Null check on the memory address
        if (address == null || address.equals(MemorySegment.NULL))
            return null;

        // Get instance from cache
        Ref ref = references.get(address);
        return ref == null ? null : ref.get();
    }

    /**
     * Get a {@link Proxy} object for the provided native memory address. If a
     * Proxy object does not yet exist for this address, a new Proxy object is
     * instantiated and added to the cache. The type of the Proxy object is
     * read from the gtype field of the native instance. Invalid references are
     * removed from the cache using a GObject toggle reference.
     *
     * @param  address  memory address of the native object
     * @param  fallback fallback constructor to use when the type is not found
     *                  in the TypeCache
     * @return a Proxy instance for the provided memory address
     */
    public static Proxy getForType(MemorySegment address,
                                   Function<MemorySegment, ? extends Proxy> fallback,
                                   boolean cache) {
        
        // Get instance from the cache
        Proxy instance = lookup(address);
        if (instance != null)
            return instance;

        // Read gclass->gtype and get constructor from the type registry
        Function<MemorySegment, ? extends Proxy> ctor =
                TypeCache.getConstructor(address, fallback);
        if (ctor == null)
            return null;

        // No instance in cache: Create a new instance
        Proxy newInstance = ctor.apply(address);

        // Null check on the new instance
        if (newInstance == null)
            return null;

        // Cache GObjects
        if (cache
            && newInstance instanceof TypeInstance ti
            && GObjects.typeCheckInstanceIsFundamentallyA(ti, GOBJECT))
            return put(address, newInstance);

        return newInstance;
    }

    /**
     * Get a {@link Proxy} object for the provided native memory address of a
     * TypeClass. If a Proxy object does not yet exist for this address, a new
     * Proxy object is instantiated and added to the cache. The type of the
     * Proxy object is read from the gtype field of the native TypeClass.
     *
     * @param  address  memory address of the native object
     * @param  fallback fallback constructor to use when the type is not found
     *                  in the TypeCache
     * @param ignored   ignored
     * @return a Proxy instance for the provided memory address
     */
    public static Proxy getForTypeClass(MemorySegment address,
                                        Function<MemorySegment, ? extends Proxy> fallback,
                                        boolean ignored) {
        // Null check
        if (address == null || MemorySegment.NULL.equals(address)) {
            GLibLogger.debug("InstanceCache.getForTypeClass: address is NULL\n");
            return null;
        }

        // Get constructor from the type registry
        Type type = new TypeClass(address).readGType();
        Function<MemorySegment, ? extends Proxy> ctor =
                TypeCache.getConstructor(type, null);
        if (ctor == null)
            return fallback.apply(address);

        // Create a new throw-away instance (without a memory address) so we
        // can get the Java Class definition.
        Proxy newInstance = ctor.apply(null);
        if (newInstance == null)
            return fallback.apply(address);

        // Get the Java proxy TypeClass definition
        Class<? extends Proxy> instanceClass = newInstance.getClass();
        Class<? extends TypeClass> typeClass = Types.getTypeClass(instanceClass);
        if (typeClass == null)
            return fallback.apply(address);

        // Use the memory address constructor to create a new instance of the
        // TypeClass
        ctor = Types.getAddressConstructor(typeClass);
        if (ctor == null)
            return fallback.apply(address);

        // Create the instance
        newInstance = ctor.apply(address);
        if (newInstance == null)
            return fallback.apply(address);

        return newInstance;
    }

    /**
     * Get a {@link Proxy} object for the provided native memory address. If a
     * Proxy object does not yet exist for this address, a new Proxy object is
     * instantiated and added to the cache. Invalid references are removed from
     * the cache using a GObject toggle reference.
     *
     * @param  address  memory address of the native object
     * @param  fallback constructor for the Java proxy object
     * @return a Proxy instance for the provided memory address
     */
    public static Proxy get(MemorySegment address,
                            Function<MemorySegment, ? extends Proxy> fallback,
                            boolean cache) {

        // Get instance from the cache
        Proxy instance = lookup(address);
        if (instance != null)
            return instance;

        // No instance in cache: Create a new instance
        Proxy newInstance = fallback.apply(address);

        // Null check on the new instance
        if (newInstance == null)
            return null;

        // Cache GObjects
        if (cache
                && newInstance instanceof TypeInstance ti
                && GObjects.typeCheckInstanceIsFundamentallyA(ti, GOBJECT))
            return put(address, newInstance);

        return newInstance;
    }
    
    /**
     * Add the new GObject instance to the cache. Floating references are
     * sunk, and a toggle reference is installed.
     *
     * @param  address     the memory address of the native instance
     * @param  object the GObject instance
     * @return the cached GObject instance
     */
    public static Proxy put(MemorySegment address, Proxy object) {
        // If it was already cached, putIfAbsent() will return the existing one
        Ref existing = references.putIfAbsent(address, new Ref.Strong(object));
        if (existing != null)
            return existing.get();

        GLibLogger.debug("New %s %ld",
                object.getClass().getName(),
                address == null ? 0L : address.address());

        // Sink floating references
        if (object instanceof Floating floatingReference)
            floatingReference.refSink();
        else if (object instanceof InitiallyUnowned floatingReference)
            floatingReference.refSink();

        // Setup a toggle ref
        addToggleRef(object);
        unref(object);

        // Register a cleaner that will remove the toggle reference
        CLEANER.register(object, new ToggleRefFinalizer(address));

        // Return the new instance.
        return object;
    }

    // Calls g_object_add_toggle_ref
    private static void addToggleRef(Proxy object) {
        try {
            g_object_add_toggle_ref.invokeExact(
                    object.handle(), toggle_notify, MemorySegment.NULL);
        } catch (Throwable _err) {
            throw new AssertionError("Unexpected exception occurred: ", _err);
        }
    }

    // Calls g_object_unref
    private static void unref(Proxy object) {
        try {
            g_object_unref.invokeExact(object.handle());
        } catch (Throwable _err) {
            throw new AssertionError("Unexpected exception occurred: ", _err);
        }
    }

    // Callback function, triggered by the toggle-notify signal
    private static void handleToggleNotify(MemorySegment ignored,
                                           MemorySegment object,
                                           int isLastRef) {
        GLibLogger.debug("Toggle %ld, is_last_ref=%d",
                object == null ? 0 : object.address(), isLastRef);
        if (isLastRef != 0)
            references.computeIfPresent(object, (_, v) -> v.asWeak());
        else
            references.computeIfPresent(object, (_, v) -> v.asStrong());
    }

    /**
     * This callback is run by the {@link Cleaner} when a {@link GObject}
     * instance has become unreachable, to remove the toggle reference. The
     * reference is removed in the default GLib MainContext.
     *
     * @param address memory address of the object instance to be cleaned
     */
    private record ToggleRefFinalizer(MemorySegment address)
            implements Runnable {

        public void run() {
            if (address == null)
                return;

            // g_object_remove_toggle_ref must be called from the main context
            var defaultContext = MainContext.default_();
            if (defaultContext != null)
                defaultContext.invoke(this::removeToggleRef);
            else
                removeToggleRef();
        }

        private boolean removeToggleRef() {
            GLibLogger.debug("Unref %ld", address.address());
            try {
                g_object_remove_toggle_ref.invokeExact(
                        address, toggle_notify, MemorySegment.NULL);
            } catch (Throwable _err) {
                throw new AssertionError("Unexpected exception occurred: ", _err);
            }
            InstanceCache.references.remove(address);
            return GLib.SOURCE_REMOVE;
        }
    }
}
