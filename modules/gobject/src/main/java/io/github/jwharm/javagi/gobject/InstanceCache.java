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

package io.github.jwharm.javagi.gobject;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.github.jwharm.javagi.base.Floating;
import io.github.jwharm.javagi.base.GLibLogger;
import io.github.jwharm.javagi.gobject.types.TypeCache;
import io.github.jwharm.javagi.gobject.types.Types;
import org.gnome.glib.Type;
import org.gnome.gobject.*;

import io.github.jwharm.javagi.base.Proxy;
import org.jetbrains.annotations.Nullable;

/**
 * Caches Proxy instances so the same instance is used for the same memory address.
 */
public class InstanceCache {

    private final static Map<MemorySegment, Proxy> strongReferences = new ConcurrentHashMap<>();
    private final static Map<MemorySegment, WeakReference<Proxy>> weakReferences = new ConcurrentHashMap<>();
    private static final Cleaner CLEANER = Cleaner.create();

    /**
     * Internal helper function to retrieve a Proxy object from the Strong/WeakReferences caches.
     * @param address get the Proxy object for this address from the cache
     * @return the instance (if found), or null (if not found)
     */
    private static Proxy get(MemorySegment address) {
        
        // Null check on the memory address
        if (address == null || address.equals(MemorySegment.NULL)) {
            return null;
        }

        // Get instance from cache
        Proxy instance = strongReferences.get(address);
        if (instance != null) {
            return instance;
        }
        WeakReference<Proxy> weakRef = weakReferences.get(address);
        if (weakRef != null && weakRef.get() != null) {
            return weakRef.get();
        }
        
        // Not found
        return null;
    }

    /**
     * Get a {@link Proxy} object for the provided native memory address. If a Proxy object does  
     * not yet exist for this address, a new Proxy object is instantiated and added to the cache. 
     * The type of the Proxy object is read from the gtype field of the native instance.
     * Invalid references are removed from the cache using a GObject toggle reference.
     * @param address  memory address of the native object
     * @param fallback fallback constructor to use when the type is not found in the TypeCache
     * @return         a Proxy instance for the provided memory address
     */
    public static Proxy getForType(MemorySegment address, Function<MemorySegment, ? extends Proxy> fallback, boolean cache) {
        
        // Get instance from the cache
        Proxy instance = get(address);
        if (instance != null) {
            return instance;
        }

        // Get constructor from the type registry
        Function<MemorySegment, ? extends Proxy> ctor = TypeCache.getConstructor(address, fallback);
        if (ctor == null) {
            return null;
        }

        // No instance in cache: Create a new instance
        Proxy newInstance = ctor.apply(address);

        // Null check on the new instance
        if (newInstance == null) {
            return null;
        }

        return cache? put(address, newInstance) : newInstance;
    }

    /**
     * Get a {@link Proxy} object for the provided native memory address of a TypeClass. If a 
     * Proxy object does not yet exist for this address, a new Proxy object is instantiated 
     * and added to the cache. The type of the Proxy object is read from the gtype field of 
     * the native TypeClass.
     * @param address  memory address of the native object
     * @param fallback fallback constructor to use when the type is not found in the TypeCache
     * @return         a Proxy instance for the provided memory address
     */
    public static Proxy getForTypeClass(MemorySegment address, Function<MemorySegment, ? extends Proxy> fallback, boolean cache) {

        // Get instance from the cache
        Proxy instance = get(address);
        if (instance != null) {
            return instance;
        }
        
        // Get constructor from the type registry
        Type type = new TypeClass(address).readGType();
        Function<MemorySegment, ? extends Proxy> ctor = TypeCache.getConstructor(type, null);
        if (ctor == null) {
            return fallback.apply(address);
        }

        // Create a new throw-away instance (without a memory address) so we can get the Java Class definition.
        Proxy newInstance = ctor.apply(null);
        if (newInstance == null) {
            return fallback.apply(address);
        }

        // Get the Java proxy TypeClass definition
        Class<? extends TypeInstance> instanceClass = ((TypeInstance) newInstance).getClass();
        Class<? extends TypeClass> typeClass = Types.getTypeClass(instanceClass);
        if (typeClass == null) {
            return fallback.apply(address);
        }

        // Use the memory address constructor to create a new instance of the TypeClass
        ctor = Types.getAddressConstructor(typeClass);
        if (ctor == null) {
            return fallback.apply(address);
        }

        // Create the instance
        newInstance = ctor.apply(address);
        if (newInstance == null) {
            return fallback.apply(address);
        }

        return cache ? put(address, newInstance) : newInstance;
    }

    /**
     * Get a {@link Proxy} object for the provided native memory address. If a Proxy object does  
     * not yet exist for this address, a new Proxy object is instantiated and added to the cache. 
     * Invalid references are removed from the cache using a GObject toggle reference.
     * @param address  memory address of the native object
     * @param fallback fallback constructor to use when the type is not found in the TypeCache
     * @return         a Proxy instance for the provided memory address
     */
    public static Proxy get(MemorySegment address, Function<MemorySegment, ? extends Proxy> fallback, boolean cache) {

        // Get instance from the cache
        Proxy instance = get(address);
        if (instance != null) {
            return instance;
        }

        // No instance in cache: Create a new instance
        Proxy newInstance = fallback.apply(address);

        // Null check on the new instance
        if (newInstance == null) {
            return null;
        }

        // Cache GTypeInstance, GTypeClass and GTypeInterface
        if (newInstance instanceof TypeInstance || newInstance instanceof TypeClass || newInstance instanceof TypeInterface) {
            return cache ? put(address, newInstance) : newInstance;
        }

        return newInstance;
    }
    
    /**
     * Add the new Proxy instance to the cache. Floating references are sinked, and for GObjects 
     * a toggle reference is installed.
     * @param address the memory address of the native instance
     * @param newInstance the Proxy instance
     * @return the cached Proxy instance
     */
    public static Proxy put(MemorySegment address, Proxy newInstance) {
        // Do not put a new instance if it already exists
        if (strongReferences.containsKey(address) || weakReferences.containsKey(address)) {
            return newInstance;
        }

        GLibLogger.debug("New %s %ld", newInstance.getClass().getName(), address == null ? 0L : address.address());

        // Put the instance in the cache. If another thread did this (while we were creating a new
        // instance), putIfAbsent() will return that instance.
        WeakReference<Proxy> existingInstance = weakReferences.putIfAbsent(address, new WeakReference<>(newInstance));
        if (existingInstance != null && existingInstance.get() != null) {
            return existingInstance.get();
        }

        // Sink floating references
        if (newInstance instanceof Floating floatingReference) {
            floatingReference.refSink();
        } else if (newInstance instanceof InitiallyUnowned floatingReference) {
            floatingReference.refSink();
        }
        
        // Setup a toggle ref on GObjects
        if (newInstance instanceof GObject gobject) {
            ToggleNotify notify = new ToggleNotifyCallback();
            gobject.addToggleRef(notify);
            gobject.unref();

            // Register a cleaner that will remove the toggle reference
            CLEANER.register(gobject, new ToggleRefFinalizer(address, notify));
        }

        // Return the new instance.
        return newInstance;
    }

    /**
     * A ToggleNotify implementation that re-uses the created function pointer
     */
    private static class ToggleNotifyCallback implements ToggleNotify {

        private MemorySegment callback;

        @Override
        public MemorySegment toCallback(Arena arena) {
            if (callback == null) {
                callback = ToggleNotify.super.toCallback(arena);
            }
            return callback;
        }

        @Override
        public void run(@Nullable MemorySegment data, GObject object, boolean isLastRef) {
            var key = object.handle();
            if (isLastRef) {
                GLibLogger.debug("Toggle %ld to weak reference (is last ref)", object.handle() == null ? 0 : object.handle().address());
                weakReferences.put(key, new WeakReference<>(object));
                strongReferences.remove(key);
            } else {
                GLibLogger.debug("Toggle %ld to strong reference", object.handle() == null ? 0 : object.handle().address());
                strongReferences.put(key, object);
                weakReferences.remove(key);
            }
        }
    }

    /**
     * This callback is run by the {@link Cleaner} when a {@link org.gnome.gobject.GObject}
     * instance has become unreachable, to remove the toggle reference.
     * @param address memory address of the object instance to be cleaned
     * @param toggleNotify the same ToggleNotify toggleNotify that was passed to
     *                     {@link GObject#addToggleRef(org.gnome.gobject.ToggleNotify)}
     */
    private record ToggleRefFinalizer(MemorySegment address, ToggleNotify toggleNotify)
            implements Runnable {

        public void run() {
            GLibLogger.debug("Unref %ld", address == null ? 0L : address.address());
            new GObject(address).removeToggleRef(toggleNotify);
            InstanceCache.weakReferences.remove(address);
        }
    }
}
