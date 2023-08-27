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

package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.Patch;
import io.github.jwharm.javagi.model.Field;
import io.github.jwharm.javagi.model.Repository;
import io.github.jwharm.javagi.model.Type;

public class GObjectPatch implements Patch {

    @Override
    public void patch(Repository repo) {
        // Remove va_list marshaller. va_list parameters are unsupported
        removeType(repo, "VaClosureMarshal");
        removeType(repo, "SignalCVaMarshaller");
        removeFunction(repo, "signal_set_va_marshaller");

        // Override with different return type
        renameMethod(repo, "TypeModule", "use", "use_type_module");

        // Make GWeakRef a generic class
        makeGeneric(repo, "WeakRef");

        // Change GInitiallyUnownedClass struct to refer to GObjectClass. Both structs
        // are identical, so this has no practical consequences, besides
        // convincing the bindings generator that GObject.InitiallyUnownedClass
        // is not a fundamental type class, but extends GObject.ObjectClass.
        var iuc = repo.namespace.registeredTypeMap.get("InitiallyUnownedClass");
        if (iuc != null) {
            iuc.fieldList.clear();
            var parent_class = new Field(iuc, "parent_class", null, null);
            parent_class.type = new Type(parent_class, "GObject.ObjectClass", "GObjectClass");
            parent_class.type.girElementType = "Record";
            parent_class.type.girElementInstance = repo.namespace.registeredTypeMap.get("ObjectClass");
            parent_class.type.init("GObject.ObjectClass");
            iuc.fieldList.add(parent_class);
        }

        // Add methods for TypeInstance to set the flag to call the parent method
        inject(repo, "TypeInstance", """
        
            private boolean callParent = false;
        
            /**
             * Set the flag that determines if for virtual method calls, {@code g_type_class_peek_parent()}
             * is used to obtain the function pointer of the parent type instead of the instance class.
             * @param callParent true if you want to call the parent vfunc instead of an overrided vfunc
             */
            @ApiStatus.Internal
            protected void callParent(boolean callParent) {
                this.callParent = callParent;
            }
        
            /**
             * Returns the flag that determines if for virtual method calls, {@code g_type_class_peek_parent()}
             * is used to obtain the function pointer of the parent type instead of the instance class.
             * @return true when parent vfunc is called instead of an overrided vfunc, or false when the
             *         overrided vfunc of the instance is called.
             */
            @ApiStatus.Internal
            public boolean callParent() {
                return this.callParent;
            }
        """);

        // Add methods for GObject
        inject(repo, "Object", """
            
            /**
             * Creates a new GObject instance of the provided GType.
             * @param objectType the GType of the new GObject
             * @return the newly created GObject instance
             */
            public static <T extends GObject> T newInstance(org.gnome.glib.Type objectType) {
                var _result = constructNew(objectType, null);
                T _object = (T) InstanceCache.getForType(_result, org.gnome.gobject.GObject::new, true);
                if (_object != null) {
                    _object.ref();
                }
                return _object;
            }
            
            /**
             * Creates a new GObject instance of the provided GType and with the provided property values.
             * @param objectType the GType of the new GObject
             * @param propertyNamesAndValues pairs of property names and values (Strings and Objects)
             * @return the newly created GObject instance
             * @throws IllegalArgumentException invalid property name
             */
            public static <T extends GObject> T newInstance(org.gnome.glib.Type objectType, Object... propertyNamesAndValues) {
                return io.github.jwharm.javagi.types.Properties.newGObjectWithProperties(objectType, propertyNamesAndValues);
            }
            
            /**
             * Gets a property of an object.
             * @param propertyName the name of the property to get
             * @return the property value
             * @throws IllegalArgumentException invalid property name
             */
            public Object getProperty(String propertyName) {
                return io.github.jwharm.javagi.types.Properties.getProperty(this, propertyName);
            }
            
            /**
             * Sets a property of an object.
             * @param propertyName the name of the property to set
             * @param value the new property value
             * @throws IllegalArgumentException invalid property name
             */
            public void setProperty(String propertyName, Object value) {
                io.github.jwharm.javagi.types.Properties.setProperty(this, propertyName, value);
            }
            
            /**
             * Connect a callback to a signal for this object. The handler will be called
             * before the default handler of the signal.
             *
             * @param detailedSignal a string of the form "signal-name::detail"
             * @param callback       the callback to connect
             * @return a SignalConnection object to track, block and disconnect the signal 
             *         connection
             */
            public <T> SignalConnection<T> connect(String detailedSignal, T callback) {
                return connect(detailedSignal, callback, false);
            }
            
            /**
             * Connect a callback to a signal for this object.
             *
             * @param detailedSignal a string of the form "signal-name::detail"
             * @param callback       the callback to connect
             * @param after          whether the handler should be called before or after
             *                       the default handler of the signal
             * @return a SignalConnection object to track, block and disconnect the signal 
             *         connection
             */
            public <T> SignalConnection<T> connect(String detailedSignal, T callback, boolean after) {
                var closure = new io.github.jwharm.javagi.util.JavaClosure(callback);
                int handlerId = GObjects.signalConnectClosure(this, detailedSignal, closure, after);
                return new SignalConnection(handle(), handlerId);
            }
            
            /**
             * Emits a signal from this object.
             * 
             * @param detailedSignal a string of the form "signal-name::detail"
             * @param params         the parameters to emit for this signal
             * @return the return value of the signal, or {@code null} if the signal 
             *         has no return value
             * @throws IllegalArgumentException if a signal with this name is not found for the object
             */
            public Object emit(String detailedSignal, Object... params) {
                return io.github.jwharm.javagi.types.Signals.emit(this, detailedSignal, params);
            }
        """);
    }
}
