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

package io.github.jwharm.javagi.gtk.types;

import io.github.jwharm.javagi.gtk.annotations.GtkTemplate;

import org.gnome.gio.Proxy;
import org.gnome.glib.Type;
import org.gnome.gobject.GObject;
import org.gnome.gobject.TypeClass;
import org.gnome.gobject.TypeFlags;
import org.gnome.gobject.TypeInstance;
import org.gnome.gtk.Widget;

import java.lang.foreign.*;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This class contains functionality to register a Java class as a Gtk
 * composite template class.
 * <p>
 * To register a Java class as a "regular" GObject class, see 
 * {@link io.github.jwharm.javagi.gobject.types.Types#register(Class)}
 *
 * @deprecated This class was renamed to {@link TemplateTypes}
 */
@Deprecated
public class Types {

    /**
     * This will call {@code TemplateTypes#registerTemplate(Class)} when
     * {@code cls} is a {@code Widget.class} with {@link GtkTemplate}
     * annotation, and
     * {@link io.github.jwharm.javagi.gobject.types.Types#register(Class)} for
     * all other (GObject-derived) classes.
     *
     * @param  cls the class to register as a new GType
     * @param  <T> the class must extend {@link org.gnome.gobject.GObject}
     * @return the new GType
     * @deprecated see {@link TemplateTypes#register(Class)}
     */
    @Deprecated
    public static <T extends GObject, W extends Widget>
    Type register(Class<T> cls) {
        return TemplateTypes.register(cls);
    }

    /**
     * Convenience function that redirects to
     * {@link io.github.jwharm.javagi.gobject.types.Types#register(Type, String, MemoryLayout, Consumer, MemoryLayout, Consumer, Function, Set)}
     *
     * @param parentType     parent GType
     * @param typeName       name of the GType
     * @param classLayout    memory layout of the typeclass
     * @param classInit      static class initializer function
     * @param instanceLayout memory layout of the typeinstance
     * @param instanceInit   static instance initializer function
     * @param constructor    memory-address constructor
     * @param flags          type flags
     * @return the new GType
     * @deprecated see {@link TemplateTypes#register(Type, String, MemoryLayout, Consumer, MemoryLayout, Consumer, Function, Set)}
     */
    @Deprecated
    public static Type register(Type parentType,
                  String typeName,
                  MemoryLayout classLayout,
                  Consumer<TypeClass> classInit,
                  MemoryLayout instanceLayout,
                  Consumer<TypeInstance> instanceInit,
                  Function<MemorySegment, ? extends Proxy> constructor,
                  Set<TypeFlags> flags) {
        return TemplateTypes.register(
                parentType, typeName, classLayout, classInit, instanceLayout,
                instanceInit, constructor, flags);
    }
}
