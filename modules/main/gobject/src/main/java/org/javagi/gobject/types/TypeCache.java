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

package org.javagi.gobject.types;

import java.lang.foreign.MemorySegment;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.gnome.glib.Type;
import org.gnome.gobject.GObjects;
import org.gnome.gobject.TypeClass;
import org.gnome.gobject.TypeInstance;

import org.javagi.base.Enumeration;
import org.javagi.base.Proxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.javagi.gobject.types.Types.IS_FUNDAMENTAL;
import static java.util.Objects.requireNonNull;

/**
 * A register of GTypes with a Java constructor for each GType.
 * Using this register, the correct Java class is always instantiated, based on
 * the GType of the native object instance.
 */
public class TypeCache {

    private final static Map<Type, Function<MemorySegment, ? extends Proxy>> typeRegister
            = new ConcurrentHashMap<>();

    private final static Map<Type, Function<MemorySegment, ? extends Proxy>> typeClassRegister
            = new ConcurrentHashMap<>();

    private final static Map<Type, Function<Integer, ? extends Enumeration>> enumTypeRegister
            = new ConcurrentHashMap<>();

    private final static Map<Class<?>, Type> classToTypeMap
            = new ConcurrentHashMap<>();

    private static final Map<Class<? extends Proxy>, Function<Class<? extends Proxy>, Type>> typeRegisterFunctions
            = new ConcurrentHashMap<>();

    public static void setTypeRegisterFunction(Class<? extends Proxy> cls,
                                               Function<Class<? extends Proxy>, Type> function) {
        typeRegisterFunctions.put(cls, function);
    }

    /**
     * Get the constructor from the type registry for the native object
     * instance at the given memory address. The applicable constructor is
     * determined based on the GType of the native object (as it was registered
     * using {@link #register(Class, Type, Function, Function)}).
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

        // Read the TypeClass from memory
        TypeClass gclass = new TypeInstance(address).readGClass();
        if (gclass == null) return null;

        // Read the gtype from memory
        Type type = gclass.readGType();
        
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
        var typeInterfaces = GObjects.typeInterfaces(type);
        if (typeInterfaces != null) {
            for (var iface : typeInterfaces) {
                var result = tryConstruct(cls, iface);
                if (result != null)
                    return result;
            }
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
     * Get a function that will create a Enumeration instance for a given int
     * value for the provided GType.
     *
     * @param type the GType of the enum/flags type
     * @return the contructor function
     * @param <T> the function will create Enum instances that implement the
     *            Java-GI Enumeration interface
     */
    @SuppressWarnings("unchecked") // // Can't get the type of the HashMap exactly right
    public static <T extends Enum<T> & Enumeration>
    Function<Integer, T> getEnumConstructor(@NotNull Type type) {
        return (Function<Integer, T>) enumTypeRegister.get(type);
    }

    /**
     * Return the GType that was registered for this class. If no type was
     * registered yet, this method will try to register it, and then return
     * the GType.
     *
     * @param  cls a Java class
     * @return the cached GType
     */
    public static Type getType(Class<?> cls) {
        requireNonNull(cls);

        // Class must be a Proxy-derived class
        @SuppressWarnings("unchecked")
        var proxyClass = (Class<? extends Proxy>) cls;

        // Ensure the class is loaded and initialized. This is useful in case
        // there's static initialization code that needs to be run.
        forceInit(cls);

        // Is the class cached?
        var type = classToTypeMap.get(cls);
        if (type != null)
            return type;

        // Register the type: Determine which function to use
        try {
            var classes = typeRegisterFunctions.keySet();
            var mostSpecific = classes.stream()
                    .filter(c -> c.isAssignableFrom(cls))
                    .max((c0, c1) -> c0.isAssignableFrom(c1) ? -1 : c1.isAssignableFrom(c0) ? 1 : 0);

            if (mostSpecific.isPresent()) {
                // Run the type registration function
                var function = typeRegisterFunctions.get(mostSpecific.get());

                function.apply(proxyClass);
                type = classToTypeMap.get(cls);
                return type;
            } else {
                // No function found to register this class: Fallback to Types.register()
                return Types.register(cls);
            }
        } catch (TypeRegistrationException cre) {
            // Type registration failed: Use the parent class GType for this
            // class. The subclass will only exist in Java; native code will
            // think it's the parent class.
            var superType = getType(cls.getSuperclass());
            register(cls, superType, null, null);
            return superType;
        }
    }

    public static Function<MemorySegment, ? extends Proxy>
    getTypeClassConstructor(@NotNull Type type) {
        return typeClassRegister.get(type);
    }

    /**
     * Register the type and constructor function for the provided class
     *
     * @param cls           Class in Java
     * @param type          The registered GType
     * @param ctor          Constructor function for this type
     * @param typeClassCtor Constructor function for the typeclass of this type
     */
    public static void register(Class<?> cls,
                                Type type,
                                Function<MemorySegment, ? extends Proxy> ctor,
                                Function<MemorySegment, ? extends Proxy> typeClassCtor) {
        requireNonNull(cls);
        if (type != null) {
            if (ctor != null)
                typeRegister.put(type, ctor);
            if (typeClassCtor != null)
                typeClassRegister.put(type, typeClassCtor);
            classToTypeMap.put(cls, type);
        }
    }

    /**
     * Register the type and constructor function for an enum/flags type
     *
     * @param cls  Class in Java
     * @param type The registered GType
     * @param ctor Constructor function for this type
     */
    public static <T extends Enum<T> & Enumeration> void registerEnum(
            Class<T> cls,
            Type type,
            Function<Integer, T> ctor) {
        requireNonNull(cls);
        if (type != null) {
            if (ctor != null)
                enumTypeRegister.put(type, ctor);
        }
        classToTypeMap.put(cls, type);
    }

    /**
     * Check if this class is already cached.
     *
     * @param  cls the class to check
     * @return true when the class is cached in the TypeCache
     */
    public static boolean contains(Class<?> cls) {
        return classToTypeMap.containsKey(cls);
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
