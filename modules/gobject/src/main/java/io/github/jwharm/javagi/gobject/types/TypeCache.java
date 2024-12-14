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

package io.github.jwharm.javagi.gobject.types;

import java.lang.foreign.MemorySegment;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.gnome.glib.Type;
import org.gnome.gobject.GObjects;
import org.gnome.gobject.TypeInstance;

import io.github.jwharm.javagi.base.Proxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static io.github.jwharm.javagi.gobject.types.Types.IS_FUNDAMENTAL;
import static java.util.Objects.requireNonNull;

/**
 * A register of GTypes with a Java constructor for each GType.
 * Using this register, the correct Java class is always instantiated, based on
 * the GType of the native object instance.
 */
public class TypeCache {
    
    private final static Map<Type, Function<MemorySegment, ? extends Proxy>> typeRegister
            = new ConcurrentHashMap<>();

    private final static Map<Class<?>, Type> classToTypeMap
            = new ConcurrentHashMap<>();

    /**
     * Get the constructor from the type registry for the native object
     * instance at the given memory address. The applicable constructor is
     * determined based on the GType of the native object (as it was registered
     * using {@link #register(Class, Type, Function)}).
     *
     * @param address  address of TypeInstance object to obtain the type from
     * @param fallback if none was found, this constructor will be registered
     *                 for the type, and returned
     * @return         the constructor, or {@code null} if address is
     *                 {@code null} or a null-pointer
     */
    public static Function<MemorySegment, ? extends Proxy>
    getConstructor(MemorySegment address,
                   Function<MemorySegment, ? extends Proxy> fallback) {
        // Null check on the memory address
        if (address == null || address.equals(MemorySegment.NULL)) return null;

        // Read the gtype from memory
        Type type = new TypeInstance(address).readGClass().readGType();
        
        return getConstructor(type, fallback);
    }
    
    /**
     * Get the constructor from the type registry for the provided GType.
     * If it isn't found, we are probably dealing with an anonymous subclass or
     * interface implementation. So we try to find a constructor for the parent
     * type and the implemented interfaces. If that works, it is registered as
     * the constructor for the GType and returned. Otherwise, the provided
     * fallback constructor is registered and returned.
     *
     * @param type     the GType for which the constructor was registered
     * @param fallback if none was found, this constructor will be registered
     *                 for the type, and returned
     * @return         the constructor, or {@code null} if address is
     *                 {@code null} or a null-pointer
     */
    public static Function<MemorySegment, ? extends Proxy> getConstructor(
            @NotNull Type type,
            @Nullable Function<MemorySegment, ? extends Proxy> fallback) {
        // Find the constructor in the typeRegister and return it
        Function<MemorySegment, ? extends Proxy> ctor = typeRegister.get(type);
        if (ctor != null)
            return ctor;

        // Get the class of the fallback constructor. Whatever constructor we
        // return, must produce instances derived from this class.
        var cls = fallback == null ? null : fallback.apply(null).getClass();

        // Check parent type, unless it is a fundamental type (like GObject),
        // which would be the most generic and useless type we can use. So in
        // that case we first try all other available options.
        var parent = GObjects.typeParent(type);
        if (!IS_FUNDAMENTAL(parent)) {
            var result = tryConstruct(cls, parent);
            if (result != null)
                return result;
        }

        // Check implemented interfaces
        for (var iface : GObjects.typeInterfaces(type)) {
            var result = tryConstruct(cls, iface);
            if (result != null)
                return result;
        }

        // Register the fallback constructor for this type
        if (fallback != null) {
            typeRegister.put(type, fallback);
            return fallback;
        }

        // No fallback was provided, return parent (fundamental type)
        return tryConstruct(null, parent);
    }

    // Register and return the constructor registered for {@code type}, if it
    // produces an instance of {@code base}.
    private static Function<MemorySegment, ? extends Proxy>
    tryConstruct(Class<?> base, Type type) {
        var ctor = typeRegister.get(type);
        if (base == null)
            return ctor;

        if (ctor != null) {
            if (base.isAssignableFrom(ctor.apply(null).getClass())) {
                typeRegister.put(type, ctor);
                return ctor;
            }
        }
        return null;
    }

    /**
     * Return the GType that was registered for this class.
     *
     * @param  cls a Java class
     * @return the cached GType
     */
    public static Type getType(Class<?> cls) {
        requireNonNull(cls);
        forceInit(cls);
        var type = classToTypeMap.get(cls);
        if (type == null)
            throw new IllegalArgumentException(
                    "Class " + cls.getSimpleName() + " is not a registered GType");
        return type;
    }

    /**
     * Register the type and constructor function for the provided class
     *
     * @param cls  Class in Java
     * @param type The registered GType
     * @param ctor Constructor function for this type
     */
    public static void register(Class<?> cls,
                                Type type,
                                Function<MemorySegment, ? extends Proxy> ctor) {
        requireNonNull(cls);
        if (type != null) {
            if (ctor != null)
                typeRegister.put(type, ctor);
            classToTypeMap.put(cls, type);
        }
    }

    /**
     * Forces the initialization of the class pertaining to the specified
     * {@code Class} object. This method does nothing if the class is already
     * initialized prior to invocation.
     *
     * @param cls the class for which to force initialization
     */
    private static void forceInit(Class<?> cls) {
        try {
            Class.forName(cls.getName(), true, cls.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }
}
