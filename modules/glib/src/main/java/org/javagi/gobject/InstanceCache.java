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

package org.javagi.gobject;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.javagi.base.Floating;
import org.javagi.base.ProxyInstance;
import org.javagi.gobject.types.TypeCache;
import org.javagi.interop.Interop;
import org.gnome.glib.MainContext;
import org.gnome.glib.Type;
import org.gnome.gobject.*;

import org.javagi.base.Proxy;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Caches TypeInstances so the same instance is used for the same memory
 * address.
 */
@NullMarked
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
                return requireNonNull(proxy.get());
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
            CONSTRUCT_STACK = ThreadLocal.withInitial(ArrayList::new);
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

    private static final VarHandle ADDRESS_FIELD;

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

        // Initialize the ADDRESS_FIELD VarHandle. This is used to update the
        // (private) address field of a newly constructed GObject instance.
        try {
            ADDRESS_FIELD = MethodHandles
                    .privateLookupIn(ProxyInstance.class, MethodHandles.lookup())
                    .findVarHandle(ProxyInstance.class, "address", MemorySegment.class);
        } catch (ReflectiveOperationException e) {
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
    private static @Nullable Proxy lookup(@Nullable MemorySegment address) {
        
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
    public static @Nullable Proxy getForType(MemorySegment address,
                                             Function<MemorySegment, ? extends Proxy> fallback) {
        
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

        // If this instance is newly constructed
        if (!constructStack.isEmpty()
                && newInstance instanceof TypeInstance ti) {
            var proxy = constructStack.peek();
            var actualType = ti.readGClass().readGType();
            var creatingType = TypeCache.getType(proxy.getClass());
            if (actualType.equals(creatingType)) {
                ADDRESS_FIELD.set(proxy, newInstance.handle());
                put(address, proxy);
                return proxy;
            }
        }

        // Cache GObject
        if (newInstance instanceof GObject gobject)
            return put(address, gobject);

        // Sink initially floating ParamSpec
        else if (newInstance instanceof ParamSpec paramSpec)
            paramSpec.refSink();

        return newInstance;
    }

    /**
     * Get a {@link Proxy} object for the provided native memory address of a
     * TypeClass. The type of the Proxy object is read from the gtype field of
     * the native TypeClass.
     *
     * @param  address  memory address of the native object
     * @param  fallback fallback constructor to use when the type is not found
     *                  in the TypeCache
     * @return a Proxy instance for the provided memory address
     */
    public static @Nullable Proxy getForTypeClass(@Nullable MemorySegment address,
                                                  Function<MemorySegment, ? extends Proxy> fallback) {
        // Don't try to dereference a null pointer
        if (address == null || MemorySegment.NULL.equals(address))
            return null;

        // Get the GType of the GTypeClass
        Type type = new TypeClass(address).readGType();
        while (type.getValue() != 0) {
            // Get the Java GTypeClass constructor for this type
            Function<MemorySegment, ? extends Proxy> constructor =
                    TypeCache.getTypeClassConstructor(type);

            if (constructor != null)
                return constructor.apply(address);

            // Not found: Try the parent type
            type = GObjects.typeParent(type);
        }

        return fallback.apply(address);
    }

    /**
     * Add the new GObject instance to the cache. Floating references are
     * sunk, and a toggle reference is installed.
     *
     * @param  address the memory address of the native instance
     * @param  object  the GObject instance
     * @return the cached GObject instance
     */
    public static Proxy put(MemorySegment address, GObject object) {
        // If it was already cached, putIfAbsent() will return the existing one
        var existing = references.putIfAbsent(address, new Ref.Strong<>(object));
        if (existing != null)
            return existing.get();

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

    /**
     * Construct a new GObject instance and set the provided Java proxy to
     * its address.
     *
     * @param proxy      the Java Proxy for the newly constructed GObject
     *                   instance
     * @param objectType the GType, if {@code null} it will be queried from
     *                   the TypeCache
     * @param size       the size of the native instance
     * @param properties pairs of property names and values (optional).
     *                   A trailing {@code null} will be added automatically.
     */
    public static void newGObject(GObject proxy,
                                  @Nullable Type objectType,
                                  long size,
                                  @Nullable Object... properties) {
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
                if (proxy.handle() == null)
                    ADDRESS_FIELD.set(proxy, address.reinterpret(size));

                // Ensure the new object is cached
                put(address, proxy);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
        } finally {
            constructStack.pop();
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
        if (isLastRef != 0)
            references.computeIfPresent(object, (_, v) -> v.asWeak());
        else
            references.computeIfPresent(object, (_, v) -> v.asStrong());
    }

    public static final MemorySegment REMOVE_TOGGLE_REF;

    // Allocate the upcall stub for the removeToggleRef callback method
    static {
        try {
            FunctionDescriptor _fdesc = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS);
            MethodHandle _handle = MethodHandles.lookup().findStatic(
                    InstanceCache.class, "removeToggleRef", _fdesc.toMethodType());
            REMOVE_TOGGLE_REF = Linker.nativeLinker().upcallStub(_handle, _fdesc, Arena.global());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static int removeToggleRef(MemorySegment address) {
        try {
            g_object_remove_toggle_ref.invokeExact(
                    address, toggle_notify, MemorySegment.NULL);
        } catch (Throwable _err) {
            throw new AssertionError("Unexpected exception occurred: ", _err);
        }
        InstanceCache.references.remove(address);
        return 0;
    }

    private static final MethodHandle g_main_context_invoke = Interop.downcallHandle(
            "g_main_context_invoke", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS,
            ValueLayout.ADDRESS, ValueLayout.ADDRESS), false);

    /**
     * This callback is run by the {@link Cleaner} when a {@link GObject}
     * instance has become unreachable, to remove the toggle reference. The
     * reference is removed in the default GLib MainContext.
     *
     * @param address memory address of the object instance to be cleaned
     */
    private record ToggleRefFinalizer(MemorySegment address) implements Runnable {
        public void run() {
            // g_object_remove_toggle_ref must be called from the main context
            var defaultContext = MainContext.default_();
            if (defaultContext != null) {
                try {
                    g_main_context_invoke.invokeExact(defaultContext.handle(),
                            REMOVE_TOGGLE_REF, address);
                } catch (Throwable _err) {
                    throw new AssertionError(_err);
                }
            } else {
                removeToggleRef(address);
            }
        }
    }
}
