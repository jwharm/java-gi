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
import java.util.ArrayList;
import java.util.Arrays;
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

    /*
     * The Ref type represents either a strong or weak reference to a Java
     * proxy for a GObject. When a toggle-notify event is received, the cached
     * Ref is toggled between strong and weak.
     */
    private sealed interface Ref<T> permits Ref.Strong, Ref.Weak {
        record Strong<T>(T proxy) implements Ref<T> {
            public T get() {
                return proxy;
            }
        }

        record Weak<T>(WeakReference<T> proxy) implements Ref<T> {
            Weak(T proxy) {
                this(new WeakReference<>(proxy));
            }
            public T get() {
                return proxy == null ? null : proxy.get();
            }
        }

        T get();

        default Ref<T> asWeak() {
            return this instanceof Weak ? this : new Weak<>(get());
        }

        default Ref<T> asStrong() {
            return this instanceof Strong ? this : new Strong<>(get());
        }
    }

    /*
     * Stack of GObjects currently under construction. For these objects a
     * Java proxy was created, but `g_object_new` has not yet completed so
     * they don't have a memory address yet.
     *
     * This is a stack, not a plain reference, because one object can trigger
     * the creation of another object during initialization.
     *
     * The stack uses a ThreadLocal variable, so concurrently created objects
     * will not interfere with each other.
     */
    private static final class ConstructStack<T> {
        private final ThreadLocal<ArrayList<T>> CONSTRUCT_STACK;

        ConstructStack() {
            CONSTRUCT_STACK = new ThreadLocal<>();
            CONSTRUCT_STACK.set(new ArrayList<>());
        }

        boolean isEmpty() {
            return CONSTRUCT_STACK.get().isEmpty();
        }

        void push(T object) {
            CONSTRUCT_STACK.get().add(object);
        }

        T peek() {
            return CONSTRUCT_STACK.get().getLast();
        }

        T pop() {
            T object = peek();
            CONSTRUCT_STACK.get().removeLast();
            return object;
        }
    }

    private static final ConstructStack<GObject> constructStack
            = new ConstructStack<>();

    private static final ConcurrentHashMap<MemorySegment, Ref<Proxy>> references
            = new ConcurrentHashMap<>();

    private static final Cleaner CLEANER = Cleaner.create();

    private static final MethodHandle g_object_new =
            Interop.downcallHandle(
                    "g_object_new",
                    FunctionDescriptor.of(ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS),
                    true);

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
        var ref = references.get(address);
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

        // If this instance is newly constructed
        if (!constructStack.isEmpty()
                && newInstance instanceof TypeInstance ti) {
            var proxy = constructStack.peek();
            var actualType = ti.readGClass().readGType();
            var creatingType = TypeCache.getType(proxy.getClass());
            if (actualType.equals(creatingType)) {
                proxy.address = newInstance.handle();
                put(address, proxy);
                constructStack.pop();
                return proxy;
            }
        }

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
     * Add the new GObject instance to the cache. Floating references are
     * sunk, and a toggle reference is installed.
     *
     * @param  address     the memory address of the native instance
     * @param  object the GObject instance
     * @return the cached GObject instance
     */
    public static Proxy put(MemorySegment address, Proxy object) {
        // If it was already cached, putIfAbsent() will return the existing one
        var existing = references.putIfAbsent(address, new Ref.Strong<>(object));
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

    public static void newGObject(GObject proxy,
                                  Type objectType,
                                  long size,
                                  Object... properties) {
        // Split varargs into first property name and the rest
        String first;
        Object[] rest;
        if (properties == null || properties.length == 0) {
            first = null;
            rest = new Object[] {};
        } else {
            if (properties.length == 1)
                throw new IllegalArgumentException("Invalid number of arguments");
            if (properties[0] instanceof String string) {
                first = string;
                rest = Arrays.copyOfRange(properties, 1, properties.length + 1);
            } else {
                throw new IllegalArgumentException("First argument is not a String");
            }
        }

        // Invoke g_object_new() and let the proxy point to its address
        try (var _arena = Arena.ofConfined()) {
            constructStack.push(proxy);
            if (objectType == null)
                objectType = TypeCache.getType(proxy.getClass());
            try {
                var address = (MemorySegment) g_object_new.invokeExact(
                        objectType.getValue().longValue(),
                        (MemorySegment) (first == null ? MemorySegment.NULL
                                : Interop.allocateNativeString(first, _arena)),
                        rest);
                if (proxy.address == null)
                    proxy.address = address.reinterpret(size);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
        }
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
