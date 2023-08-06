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

package io.github.jwharm.javagi.types;

import java.lang.foreign.MemorySegment;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.gnome.glib.Type;
import org.gnome.gobject.TypeInstance;

import io.github.jwharm.javagi.base.Proxy;

/**
 * A register of GTypes with a Java constructor for each GType.
 * Using this register, the correct Java class is always instantiated, based on the GType of 
 * the native object instance.
 */
public class TypeCache {
    
    private final static Map<Type, Function<MemorySegment, ? extends Proxy>> typeRegister = new ConcurrentHashMap<>();

    /**
     * Get the constructor from the type registry for the native object instance at the given 
     * memory address. The applicable constructor is determined based on the GType of the native 
     * object (as it was registered using {@link #register(Type, Function)}).
     * @param address  address of TypeInstance object to obtain the type from
     * @param fallback if none was found, this constructor will be registered for the type, and returned
     * @return         the constructor, or {@code null} if address is {@code null} or a null-pointer
     */
    public static Function<MemorySegment, ? extends Proxy> getConstructor(MemorySegment address, Function<MemorySegment, ? extends Proxy> fallback) {
        // Null check on the memory address
        if (address == null || address.equals(MemorySegment.NULL)) return null;

        // Read the gtype from memory
        Type type = new TypeInstance(address).readGClass().readGType();
        
        return getConstructor(type, fallback);
    }
    
    /**
     * Get the constructor from the type registry for the provided GType.
     * @param type     the GType for which the constructor was registered
     * @param fallback if none was found, this constructor will be registered for the type, and returned
     * @return         the constructor, or {@code null} if address is {@code null} or a null-pointer
     */
    public static Function<MemorySegment, ? extends Proxy> getConstructor(Type type, Function<MemorySegment, ? extends Proxy> fallback) {
        // Find the constructor in the typeRegister and return it
        if (type != null) {
            Function<MemorySegment, ? extends Proxy> ctor = typeRegister.get(type);
            if (ctor != null) {
                return ctor;
            }
        }

        // Register the fallback constructor for this type. If another thread did this in the meantime, putIfAbsent()
        // will return that constructor.
        if (fallback != null) {
            Function<MemorySegment, ? extends Proxy> ctorFromAnotherThread = typeRegister.putIfAbsent(type, fallback);
            return Objects.requireNonNullElse(ctorFromAnotherThread, fallback);
        }
        // No constructor found in the typeRegister, and no fallback provided
        return null;
    }

    /**
     * Register the provided marshal function for the provided type
     * @param type    Type to use as key in the type register
     * @param marshal Marshal function for this type
     */
    public static void register(Type type, Function<MemorySegment, ? extends Proxy> marshal) {
        if (type != null) {
            typeRegister.put(type, marshal);
        }
    }
}
