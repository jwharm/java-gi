/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2026 Jan-Willem Harmannij
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

package org.javagi.patches;

import org.javagi.configuration.ClassNames;
import org.javagi.gir.*;
import org.javagi.gir.Class;
import org.javagi.gir.Record;
import org.javagi.util.Patch;

import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.javagi.util.Conversions.nullable;

public class GObjectPatch implements Patch {
    @Override
    public void patchRepository(Repository repository) {
        var ns = repository.namespace();
        if (!"GObject".equals(ns.name()))
            return;

        /*
         * Change GInitiallyUnownedClass struct to refer to GObjectClass. Both
         * structs are identical, so this has no practical consequences,
         * besides convincing the bindings generator that
         * GObject.InitiallyUnownedClass is not a fundamental type class, but
         * extends GObject.ObjectClass.
         */
        var initiallyUnowned = (Record) ns.select("InitiallyUnownedClass#record").getFirst();
        Type type = new Type(Map.of("name", "GObject.ObjectClass", "c:type", "GObjectClass"), emptyList());
        Field field = new Field(Map.of("name", "parent_class"), List.of(type));
        initiallyUnowned.withChildren(initiallyUnowned.infoElements().doc(), field);

        // Inject methods in GObject class
        var gobject = (Class) ns.select("Object#class").getFirst();
        inject(gobject, """
            ///
            /// Create a new GObject instance of the provided GType and with the
            /// provided property values.
            ///
            /// @param  objectType the GType of the new GObject
            /// @param  propertyNamesAndValues pairs of property names and values
            ///         (Strings and Objects)
            /// @return the newly created GObject instance
            /// @throws IllegalArgumentException invalid property name
            ///
            public static <T extends $T> T newInstance($T objectType,
                    Object... propertyNamesAndValues) {
                var constructor = $T.getConstructor(objectType, null);
                var proxy = (T) constructor.apply($T.NULL);
                $T.newGObject(proxy, objectType, getMemoryLayout().byteSize(), propertyNamesAndValues);
                return proxy;
            }
            """, ClassNames.G_OBJECT, ClassNames.G_TYPE, ClassNames.TYPE_CACHE,
                MemorySegment.class, ClassNames.INSTANCE_CACHE);

        inject(gobject, """
            ///
            /// Create a new instance of a GObject-derived class with the provided
            /// property values.
            ///
            /// @param  objectClass the Java class of the new GObject
            /// @param  propertyNamesAndValues pairs of property names and values
            ///         (Strings and Objects)
            /// @return the newly created GObject instance
            /// @throws IllegalArgumentException invalid property name
            ///
            public static <T extends $T> T newInstance(Class<T> objectClass,
                    Object... propertyNamesAndValues) {
                return newInstance($T.getType(objectClass), propertyNamesAndValues);
            }
            """, ClassNames.G_OBJECT, ClassNames.TYPE_CACHE);

        inject(gobject, """
            ///
            /// Get a property of an object.
            ///
            /// @param  propertyName the name of the property to get
            /// @return the property value
            /// @throws IllegalArgumentException invalid property name
            ///
            public $T getProperty(String propertyName) {
                return $T.getProperty(this, propertyName);
            }
            """, nullable(Object.class), ClassNames.PROPERTIES);

        inject(gobject, """
            ///
            /// Set a property of an object.
            ///
            /// @param  propertyName the name of the property to set
            /// @param  value the new property value
            /// @throws IllegalArgumentException invalid property name
            ///
            public void setProperty(String propertyName, $T value) {
                $T.setProperty(this, propertyName, value);
            }
            """, nullable(Object.class), ClassNames.PROPERTIES);

        inject(gobject, """
            ///
            /// Create a binding between `sourceProperty` on this object and
            /// `targetProperty` on `target`.
            ///
            /// Whenever the `sourceProperty` is changed the `targetProperty`
            /// is updated using the same value. For instance:
            ///
            /// ```java
            /// action.bindProperty("active", widget, "sensitive").build();
            /// ```
            ///
            /// Will result in the "sensitive" property of the widget `GObject`
            /// instance to be updated with the same value of the "active" property of
            /// the action `GObject` instance.
            ///
            /// If [BindingBuilder#bidirectional] is set then the binding will be
            /// mutual: if `targetProperty` on `target` changes then the
            /// `sourceProperty` on this Object will be updated as well.
            ///
            /// The binding will automatically be removed when either this Object
            /// or the `target` instances are finalized.
            ///
            /// A `GObject` can have multiple bindings.
            ///
            /// @param  <S> type of the source property
            /// @param  <T> type of the target property
            /// @param  sourceProperty the property on this Object to bind
            /// @param  target         the target `GObject`
            /// @param  targetProperty the property on `target` to bind
            /// @return the `GBinding` instance representing the binding between
            ///         the two `GObject` instances. The binding is released
            ///         whenever the `GBinding` reference count reaches zero.
            ///
            public <S, T> $1T<S, T> bindProperty(String sourceProperty, $2T target,
                    String targetProperty) {
                return new $1T<S, T>(this, sourceProperty, target, targetProperty);
            }
            """, ClassNames.BINDING_BUILDER, ClassNames.G_OBJECT);

        inject(gobject, """
            ///
            /// Connect a callback to a signal for this object. The handler will be
            /// called before the default handler of the signal.
            ///
            /// @param  <C>            type of the signal callback
            /// @param  detailedSignal a string of the form "signal-name::detail"
            /// @param  callback       the callback to connect
            /// @return a SignalConnection object to track, block and disconnect the
            ///         signal connection
            ///
            public <C> $T<C> connect(String detailedSignal, C callback) {
                return connect(detailedSignal, callback, false);
            }
            """, ClassNames.SIGNAL_CONNECTION);

        inject(gobject, """
            ///
            /// Connect a callback to a signal for this object.
            ///
            /// @param <C>            type of the signal callback
            /// @param detailedSignal a string of the form "signal-name::detail"
            /// @param callback       the callback to connect
            /// @param after          whether the handler should be called before or
            ///                       after the default handler of the signal
            /// @return a SignalConnection object to track, block and disconnect the
            ///         signal connection
            ///
            public <C> $1T<C> connect(String detailedSignal, C callback, boolean after) {
                $2T closure = new $2T(callback).ignoreFirstParameter();
                int handlerId = $3T.signalConnectClosure(this, detailedSignal, closure, after);
                return new $1T<C>(handle(), handlerId, closure);
            }
            """, ClassNames.SIGNAL_CONNECTION, ClassNames.JAVA_CLOSURE, ClassNames.G_OBJECTS);

        inject(gobject, """
            ///
            /// Emit a signal from this object.
            ///
            /// @param  detailedSignal a string of the form "signal-name::detail"
            /// @param  params         the parameters to emit for this signal
            /// @return the return value of the signal, or `null` if the signal
            ///         has no return value
            /// @throws IllegalArgumentException if a signal with this name is not found
            ///         for the object
            ///
            public Object emit(String detailedSignal, Object... params) {
                return $T.emit(this, detailedSignal, params);
            }
            """, ClassNames.SIGNALS);

        // Inject method in GParamSpec class
        for (Node node : ns.select("ParamSpec*#class"))
            if (node instanceof Class paramSpec)
                inject(paramSpec, """
                    ///
                    /// Get the GType of the $3L class.
                    ///
                    /// @return always {@link $1T#PARAM}
                    ///
                    public static $2T getType() {
                        return $1T.PARAM;
                    }
                    """, ClassNames.TYPES, ClassNames.G_TYPE, paramSpec.cType());

        // Inject methods in GTypeInstance class
        var typeInstance = (Record) ns.select("TypeInstance#record").getFirst();
        inject(typeInstance, """
            private boolean callParent = false;
            """);

        inject(typeInstance, """
            ///
            /// Set the flag that determines if for virtual method calls,
            /// {@code g_type_class_peek_parent()} is used to obtain the function pointer of the
            /// parent type instead of the instance class.
            ///
            /// @param callParent true to call the parent vfunc instead of an overridden vfunc
            ///
            protected void callParent(boolean callParent) {
                this.callParent = callParent;
            }
            """);

        inject(typeInstance, """
            ///
            /// Returns the flag that determines if for virtual method calls,
            /// {@code g_type_class_peek_parent()} is used to obtain the function pointer of the
            /// parent type instead of the instance class.
            ///
            /// @return true when parent vfunc is called instead of an overridden vfunc, or
            ///         false when the overridden vfunc of the instance is called.
            ///
            public boolean callParent() {
                return this.callParent;
            }
            """);

        inject(typeInstance, """
            ///
            /// Cast this instance to the requested type, if the GTypes are compatible.
            ///
            /// @param to the intended class
            /// @param <T> the type of the intended class (must be a GTypeInstance)
            /// @return a new instance of the requested class
            /// @throws $1T when {@code to} is not a registered GType
            /// @throws $2T when the GType of this instance does not derive
            ///                            from the GType of {@code to}
            ///
            public <T extends $3T> T cast(Class<T> to) {
                $6T fromType = readGClass().readGType();
                $6T toType = $4T.getType(to);
                if (toType == null) {
                    throw new $1T(to.getName() + " is not a registered GType");
                }
                if (! $5T.typeIsA(fromType, toType)) {
                    throw new $2T(fromType + " is not a " + toType);
                }
                return (T) $4T.getConstructor(toType, null).apply(handle());
            }
            """, IllegalArgumentException.class, ClassCastException.class, ClassNames.G_TYPE_INSTANCE,
                ClassNames.TYPE_CACHE, ClassNames.G_OBJECTS, ClassNames.G_TYPE);
    }
}
