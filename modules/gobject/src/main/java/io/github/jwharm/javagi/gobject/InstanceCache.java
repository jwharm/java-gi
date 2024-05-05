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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.github.jwharm.javagi.base.Floating;
import io.github.jwharm.javagi.base.GLibLogger;
import io.github.jwharm.javagi.gobject.types.TypeCache;
import io.github.jwharm.javagi.gobject.types.Types;
import io.github.jwharm.javagi.interop.Interop;
import org.gnome.glib.Type;
import org.gnome.gobject.*;

import io.github.jwharm.javagi.base.Proxy;

/**
 * Caches Proxy instances so the same instance is used for the same memory
 * address.
 */
public class InstanceCache {

    private final static Map<MemorySegment, Proxy> strongReferences = new ConcurrentHashMap<>();
    private final static Map<MemorySegment, WeakReference<Proxy>> weakReferences = new ConcurrentHashMap<>();
    private static final Cleaner CLEANER = Cleaner.create();

    static {
        GObjects.javagi$ensureInitialized();
    }

    /**
     * Internal helper function to retrieve a Proxy object from the
     * Strong/WeakReferences caches.
     *
     * @param  address get the Proxy object for this address from the cache
     * @return the instance (if found), or null (if not found)
     */
    private static Proxy get(MemorySegment address) {
        
        // Null check on the memory address
        if (address == null || address.equals(MemorySegment.NULL))
            return null;

        // Get instance from cache
        Proxy instance = strongReferences.get(address);
        if (instance != null)
            return instance;

        WeakReference<Proxy> weakRef = weakReferences.get(address);
        if (weakRef != null && weakRef.get() != null)
            return weakRef.get();

        // Not found
        return null;
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
        Proxy instance = get(address);
        if (instance != null)
            return instance;

        // Get constructor from the type registry
        Function<MemorySegment, ? extends Proxy> ctor = TypeCache.getConstructor(address, fallback);
        if (ctor == null)
            return null;

        // No instance in cache: Create a new instance
        Proxy newInstance = ctor.apply(address);

        // Null check on the new instance
        if (newInstance == null)
            return null;

        // Cache GObjects
        if (newInstance instanceof GObject gobject)
            return cache ? put(address, gobject) : newInstance;

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
     * @return a Proxy instance for the provided memory address
     */
    public static Proxy getForTypeClass(MemorySegment address,
                                        Function<MemorySegment, ? extends Proxy> fallback,
                                        boolean cache) {
        // Null check
        if (address == null || MemorySegment.NULL.equals(address)) {
            GLibLogger.debug("InstanceCache.getForTypeClass: address is NULL\n");
            return null;
        }

        // Get constructor from the type registry
        Type type = new TypeClass(address).readGType();
        Function<MemorySegment, ? extends Proxy> ctor = TypeCache.getConstructor(type, null);
        if (ctor == null)
            return fallback.apply(address);

        // Create a new throw-away instance (without a memory address) so we
        // can get the Java Class definition.
        Proxy newInstance = ctor.apply(null);
        if (newInstance == null)
            return fallback.apply(address);

        // Get the Java proxy TypeClass definition
        Class<? extends TypeInstance> instanceClass = ((TypeInstance) newInstance).getClass();
        Class<? extends TypeClass> typeClass = Types.getTypeClass(instanceClass);
        if (typeClass == null)
            return fallback.apply(address);

        // Use the memory address constructor to create a new instance of the TypeClass
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
        Proxy instance = get(address);
        if (instance != null)
            return instance;

        // No instance in cache: Create a new instance
        Proxy newInstance = fallback.apply(address);

        // Null check on the new instance
        if (newInstance == null)
            return null;

        // Cache GObjects
        if (newInstance instanceof GObject gobject)
            return cache ? put(address, gobject) : newInstance;

        return newInstance;
    }
    
    /**
     * Add the new GObject instance to the cache. Floating references are
     * sinked, and a toggle reference is installed.
     *
     * @param  address     the memory address of the native instance
     * @param  newInstance the GObject instance
     * @return the cached GObject instance
     */
    public static Proxy put(MemorySegment address, GObject newInstance) {
        // Do not put a new instance if it already exists
        if (strongReferences.containsKey(address) || weakReferences.containsKey(address))
            return newInstance;

        GLibLogger.debug("New %s %ld",
                newInstance.getClass().getName(),
                address == null ? 0L : address.address());

        // Put the instance in the cache. If another thread did this (while we
        // were creating a new instance), putIfAbsent() will return that
        // instance.
        Proxy existingInstance = strongReferences.putIfAbsent(address, newInstance);
        if (existingInstance != null)
            return existingInstance;

        // Sink floating references
        if (newInstance instanceof Floating floatingReference)
            floatingReference.refSink();
        else if (newInstance instanceof InitiallyUnowned floatingReference)
            floatingReference.refSink();

        // Setup a toggle ref
        addToggleRef(newInstance);
        newInstance.unref();

        // Register a cleaner that will remove the toggle reference
        CLEANER.register(newInstance, new ToggleRefFinalizer(address));

        // Return the new instance.
        return newInstance;
    }

    // Calls g_object_add_toggle_ref
    private static void addToggleRef(GObject object) {
        try {
            g_object_add_toggle_ref.invokeExact(object.handle(), toggleNotifyUpcall, MemorySegment.NULL);
        } catch (Throwable _err) {
            throw new AssertionError("Unexpected exception occurred: ", _err);
        }
    }

    private static final MethodHandle g_object_add_toggle_ref = Interop.downcallHandle(
            "g_object_add_toggle_ref",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
            false
    );

    // Callback function, triggered by the toggle-notify signal
    private static void handleToggleNotify(MemorySegment data,
                                           MemorySegment object,
                                           int isLastRef) {
        if (isLastRef != 0) {
            Proxy proxy = strongReferences.remove(object);
            GLibLogger.debug("Toggle %ld to weak reference (is last ref)",
                    object == null ? 0 : object.address());
            weakReferences.put(object, new WeakReference<>(proxy));
        } else {
            WeakReference<Proxy> proxy = weakReferences.remove(object);
            GLibLogger.debug("Toggle %ld to strong reference",
                    object == null ? 0 : object.address());
            strongReferences.put(object, proxy.get());
        }
    }

    // Upcall stub for handleToggleNotify()
    private static final MemorySegment toggleNotifyUpcall = allocateToggleNotifyUpcall();

    // Allocates the upcall stub for handleToggleNotify()
    private static MemorySegment allocateToggleNotifyUpcall() {
        FunctionDescriptor fdesc = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS,
                ValueLayout.ADDRESS, ValueLayout.JAVA_INT);
        try {
            var handle = MethodHandles.lookup().findStatic(
                    InstanceCache.class,
                    "handleToggleNotify",
                    fdesc.toMethodType()
            );
            return Linker.nativeLinker().upcallStub(handle, fdesc, Arena.global());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This callback is run by the {@link Cleaner} when a
     * {@link org.gnome.gobject.GObject} instance has become unreachable, to
     * remove the toggle reference.
     *
     * @param address memory address of the object instance to be cleaned
     */
    private record ToggleRefFinalizer(MemorySegment address) implements Runnable {

        public void run() {
            GLibLogger.debug("Unref %ld", address == null ? 0L : address.address());
            try {
                g_object_remove_toggle_ref.invokeExact(address, toggleNotifyUpcall, MemorySegment.NULL);
            } catch (Throwable _err) {
                throw new AssertionError("Unexpected exception occurred: ", _err);
            }
            InstanceCache.weakReferences.remove(address);
        }
    }

    private static final MethodHandle g_object_remove_toggle_ref = Interop.downcallHandle(
            "g_object_remove_toggle_ref",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
            false
    );
}
