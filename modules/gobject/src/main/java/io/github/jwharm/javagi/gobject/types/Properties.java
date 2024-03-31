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

import io.github.jwharm.javagi.gobject.annotations.Property;
import io.github.jwharm.javagi.base.Proxy;
import io.github.jwharm.javagi.base.ProxyInstance;
import io.github.jwharm.javagi.gobject.ValueUtil;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.gnome.gobject.*;

import java.lang.foreign.Arena;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static io.github.jwharm.javagi.Constants.LOG_DOMAIN;

/**
 * Helper class to register properties in a new GType.
 */
public class Properties {

    /**
     * Read the GType of the GParamSpec of a GObject property.
     *
     * @param  objectClass  the GObject typeclass that has a property installed
     *                      with the provided name
     * @param  propertyName the name of the property
     * @return the GType of the GParamSpec of the GObject property, or null if
     *         not found
     */
    public static Type readPropertyValueType(GObject.ObjectClass objectClass, String propertyName) {
        ParamSpec pspec = objectClass.findProperty(propertyName);
        if (pspec == null) {
            throw new IllegalArgumentException("Cannot find property \"%s\" for type %s\n"
                    .formatted(propertyName, GObjects.typeName(objectClass.readGType())));
        }
        ParamSpec.ParamSpecClass pclass = (ParamSpec.ParamSpecClass) pspec.readGClass();
        return pclass == null ? null : pclass.readValueType();
    }

    /**
     * Set a property of an object.
     *
     * @param  propertyName the name of the property to set
     * @param  propertyValue the new property propertyValue
     * @throws IllegalArgumentException if a property with this name is not found for the object
     */
    public static void setProperty(GObject gobject, String propertyName, Object propertyValue) {
        GObject.ObjectClass gclass = (GObject.ObjectClass) gobject.readGClass();
        Type valueType = readPropertyValueType(gclass, propertyName);
        try (var arena = Arena.ofConfined()) {
            var gvalue = new Value(arena).init(valueType);
            ValueUtil.objectToValue(propertyValue, gvalue);
            gobject.setProperty(propertyName, gvalue);
            gvalue.unset();
        }
    }

    /**
     * Get a property of an object.
     *
     * @param  gobject      the object instance
     * @param  propertyName the name of the property to get
     * @return the property value
     * @throws IllegalArgumentException if a property with this name is not
     *                                  found for the object
     */
    public static Object getProperty(GObject gobject, String propertyName) {
        GObject.ObjectClass gclass = (GObject.ObjectClass) gobject.readGClass();
        Type valueType = readPropertyValueType(gclass, propertyName);
        try (var arena = Arena.ofConfined()) {
            var gvalue = new Value(arena).init(valueType);
            gobject.getProperty(propertyName, gvalue);
            Object result = ValueUtil.valueToObject(gvalue);
            gvalue.unset();
            return result;
        }
    }

    /**
     * Create a new GObject instance of the provided GType and with the
     * provided property values.
     *
     * @param  objectType             the GType of the new GObject
     * @param  propertyNamesAndValues pairs of property names and values
     *                                (Strings and Objects)
     * @return the newly created GObject instance
     * @throws IllegalArgumentException if a property with this name is not
     *                                  found for the object
     */
    public static <T extends GObject> T newGObjectWithProperties(Type objectType, Object... propertyNamesAndValues) {
        List<String> names = new ArrayList<>();
        List<Value> values = new ArrayList<>();
        TypeClass typeClass = TypeClass.ref(objectType);

        if (!(typeClass instanceof GObject.ObjectClass objectClass))
            throw new IllegalArgumentException("Type %s is not a GObject class"
                    .formatted(GObjects.typeName(objectType)));

        try (var arena = Arena.ofConfined()) {
            try {
                for (int i = 0; i < propertyNamesAndValues.length; i++) {

                    // Odd number of parameters?
                    if (i == propertyNamesAndValues.length - 1) {
                        if (propertyNamesAndValues[i] == null) {
                            // Ignore a closing null parameter (often expected by
                            // GObject vararg functions)
                            break;
                        }
                        throw new IllegalArgumentException("Argument list must contain pairs of property names and values");
                    }

                    // Get the name of the property
                    if (propertyNamesAndValues[i] instanceof String name) {
                        names.add(name);
                    } else {
                        throw new IllegalArgumentException("Property name is not a String: %s"
                                .formatted(propertyNamesAndValues[i]));
                    }

                    // The value for the property is a java object, and must be
                    // converted to a GValue
                    Object object = propertyNamesAndValues[++i];

                    // Read the objectType of GValue that is expected for this
                    // property
                    Type valueType = readPropertyValueType(objectClass, name);

                    // Create a GValue and write the object to it
                    Value gvalue = new Value(arena).init(valueType);
                    ValueUtil.objectToValue(object, gvalue);
                    values.add(gvalue);
                }

                /*
                 * Create and return the GObject with the property names and values
                 * The cast to T is safe: it will always return the expected
                 * GObject-derived objectType
                 */
                @SuppressWarnings("unchecked")
                T gobject = (T) GObject.withProperties(
                        objectType,
                        names.toArray(new String[0]),
                        values.toArray(new Value[0])
                );
                return gobject;
            } finally {
                typeClass.unref();
                values.forEach(Value::unset);
            }
        }
    }

    /*
     * Convert "CamelCase" to "kebab-case"
     */
    private static String getPropertyName(String methodName) {
        if (methodName.startsWith("get") || methodName.startsWith("set")) {
            String value = methodName.substring(3);
            return value.replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                    .toLowerCase().replaceAll("\\.", "");
        }
        throw new IllegalArgumentException("Cannot infer property name from method named %s"
                .formatted(methodName));
    }

    /*
     * Infer the ParamSpec class from the Java class that is used in the
     * getter/setter method.
     */
    private static Class<? extends ParamSpec> inferType(Method method) {
        // Determine the Java class of the property
        Class<?> paramClass;
        if ((! method.getReturnType().equals(void.class))
                && method.getParameterCount() == 0) {
            // Getter
            paramClass = method.getReturnType();
        } else if (method.getReturnType().equals(void.class)
                && method.getParameterCount() == 1) {
            // Setter
            paramClass = method.getParameterTypes()[0];
        } else {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Invalid property getter/setter %s in class %s\n",
                    method.getName(), method.getDeclaringClass().getName());
            return null;
        }

        // Infer the ParamSpec from the Java class.
        if (paramClass.equals(boolean.class) || paramClass.equals(Boolean.class))
            return ParamSpecBoolean.class;

        else if (paramClass.equals(byte.class) || paramClass.equals(Byte.class))
            return ParamSpecChar.class;

        else if (paramClass.equals(char.class) || paramClass.equals(Character.class))
            return ParamSpecChar.class;

        else if (paramClass.equals(double.class) || paramClass.equals(Double.class))
            return ParamSpecDouble.class;

        else if (paramClass.equals(float.class) || paramClass.equals(Float.class))
            return ParamSpecFloat.class;

        else if (paramClass.equals(int.class) || paramClass.equals(Integer.class))
            return ParamSpecInt.class;

        else if (paramClass.equals(long.class) || paramClass.equals(Long.class))
            return ParamSpecLong.class;

        else if (paramClass.equals(String.class))
            return ParamSpecString.class;

        else if (Type.class.isAssignableFrom(paramClass))
            return ParamSpecGType.class;

        // GObject class
        else if (GObject.class.isAssignableFrom(paramClass))
            return ParamSpecObject.class;

        // Struct
        else if (ProxyInstance.class.isAssignableFrom(paramClass))
            return ParamSpecBoxed.class;

        // GObject interface
        else if (Proxy.class.isAssignableFrom(paramClass))
            return ParamSpecObject.class;

        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                "Invalid property type %s in method %s in class %s\n",
                paramClass.getName(), method.getName(), method.getDeclaringClass().getName());
        return null;
    }

    /*
     * Create a GParamFlags based on {@code @Property} annotation parameters.
     */
    private static ParamFlags getFlags(Property property) {
        ParamFlags flags = new ParamFlags(0);
        if (property.readable())       flags = flags.or(ParamFlags.READABLE);
        if (property.writable())       flags = flags.or(ParamFlags.WRITABLE);
        if (property.construct())      flags = flags.or(ParamFlags.CONSTRUCT);
        if (property.constructOnly())  flags = flags.or(ParamFlags.CONSTRUCT_ONLY);
        if (property.explicitNotify()) flags = flags.or(ParamFlags.EXPLICIT_NOTIFY);
        if (property.deprecated())     flags = flags.or(ParamFlags.DEPRECATED);
        return flags;
    }

    /**
     * If the provided class defines {@code @Property}-annotated getter and/or
     * setter methods, this function will return a class initializer that
     * registers these properties as GObject properties and overrides the
     * {@code GObject.getProperty} and {@code setProperty} methods to call the
     * annotated getters and setters.
     *
     * @param  cls  the class that possibly contains @Property annotations
     * @param  <T>  the class must extend {@link org.gnome.gobject.GObject}
     * @param  <TC> the returned lambda expects a {@link GObject.ObjectClass}
     *              parameter
     * @return a class initializer that registers the properties
     */
    public static <T extends GObject, TC extends GObject.ObjectClass> Consumer<TC> installProperties(Class<T> cls) {
        List<ParamSpec> propertySpecs = new ArrayList<>();
        propertySpecs.add(null); // Index 0 is reserved

        /*
         * Create an index of property names. The list is used to obtain a
         * property id using `list.indexOf(property.name())`
         */
        List<String> propertyNames = new ArrayList<>();
        propertyNames.add(null); // index 0 is reserved

        for (Method method : cls.getDeclaredMethods()) {

            // Look for methods with annotation @Property
            Property p = method.getAnnotation(Property.class);
            if (p == null)
                continue;

            // Name is specified with the annotation, or infer it from the method name
            String name = p.name().isEmpty()
                    ? getPropertyName(method.getName())
                    : p.name();

            // Check if this property has already been added from another method
            if (propertyNames.contains(name))
                continue;

            Class<? extends ParamSpec> paramspec = p.type();

            /*
             * Check if the type is set on this method. It can be set on either
             * the getter or setter. If the type is not set, it defaults to
             * ParamSpec.class
             */
            if (paramspec.equals(ParamSpec.class))
                paramspec = inferType(method);

            if (paramspec == null)
                continue;

            ParamSpec ps;
            if (paramspec.equals(ParamSpecBoolean.class))
                ps = GObjects.paramSpecBoolean(name, name, name,
                        false, getFlags(p));

            else if (paramspec.equals(ParamSpecChar.class))
                ps = GObjects.paramSpecChar(name, name, name,
                        Byte.MIN_VALUE, Byte.MAX_VALUE, (byte) 0, getFlags(p));

            else if (paramspec.equals(ParamSpecDouble.class))
                ps = GObjects.paramSpecDouble(name, name, name,
                        -Double.MAX_VALUE, Double.MAX_VALUE, 0.0d, getFlags(p));

            else if (paramspec.equals(ParamSpecFloat.class))
                ps = GObjects.paramSpecFloat(name, name, name,
                        -Float.MAX_VALUE, Float.MAX_VALUE, 0.0f, getFlags(p));

            else if (paramspec.equals(ParamSpecGType.class))
                ps = GObjects.paramSpecGtype(name, name, name,
                        Types.NONE, getFlags(p));

            else if (paramspec.equals(ParamSpecInt.class))
                ps = GObjects.paramSpecInt(name, name, name,
                        Integer.MIN_VALUE, Integer.MAX_VALUE, 0, getFlags(p));

            else if (paramspec.equals(ParamSpecInt64.class))
                ps = GObjects.paramSpecInt64(name, name, name,
                        Long.MIN_VALUE, Long.MAX_VALUE, 0, getFlags(p));

            else if (paramspec.equals(ParamSpecLong.class))
                ps = GObjects.paramSpecLong(name, name, name,
                        Integer.MIN_VALUE, Integer.MAX_VALUE, 0, getFlags(p));

            else if (paramspec.equals(ParamSpecPointer.class))
                ps = GObjects.paramSpecPointer(name, name, name,
                        getFlags(p));

            else if (paramspec.equals(ParamSpecString.class))
                ps = GObjects.paramSpecString(name, name, name,
                        null, getFlags(p));

            else if (paramspec.equals(ParamSpecUChar.class))
                ps = GObjects.paramSpecUchar(name, name, name,
                        (byte) 0, Byte.MAX_VALUE, (byte) 0, getFlags(p));

            else if (paramspec.equals(ParamSpecUInt.class))
                ps = GObjects.paramSpecUint(name, name, name,
                        0, Integer.MAX_VALUE, 0, getFlags(p));

            else if (paramspec.equals(ParamSpecUInt64.class))
                ps = GObjects.paramSpecUint64(name, name, name,
                        0, Long.MAX_VALUE, 0, getFlags(p));

            else if (paramspec.equals(ParamSpecULong.class))
                ps = GObjects.paramSpecUlong(name, name, name,
                        0, Integer.MAX_VALUE, 0, getFlags(p));

            else if (paramspec.equals(ParamSpecUnichar.class))
                ps = GObjects.paramSpecUnichar(name, name, name,
                        0, getFlags(p));

            else {
                GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                        "Unsupported ParamSpec %s in class %s:\n",
                        paramspec.getName(), cls.getName());
                return null;
            }
            propertySpecs.add(ps);
            propertyNames.add(name);
        }

        // No properties found?
        if (propertySpecs.size() == 1)
            return null;

        // Create arrays of getter and setter methods.
        Method[] getters = new Method[propertySpecs.size()];
        Method[] setters = new Method[propertySpecs.size()];

        for (Method method : cls.getDeclaredMethods()) {
            if (! method.isAnnotationPresent(Property.class)) {
                continue;
            }
            Property property = method.getDeclaredAnnotation(Property.class);

            // Name is specified with the annotation, or infer it form the method name
            String name = property.name().isEmpty()
                    ? getPropertyName(method.getName())
                    : property.name();

            int idx = propertyNames.indexOf(name);

            // Returns void -> setter, else -> getter
            if (method.getReturnType().equals(void.class))
                setters[idx] = method;
            else
                getters[idx] = method;
        }

        // Create GParamSpec array. Index 0 is reserved.
        final ParamSpec[] pspecs = new ParamSpec[propertySpecs.size()];
        for (int i = 1; i < propertySpecs.size(); i++) {
            pspecs[i] = propertySpecs.get(i);
        }

        // Return class initializer method that installs the properties.
        return (gclass) -> {
            // Override the get_property virtual method
            gclass.overrideGetProperty((object, propertyId, value, pspec) -> {

                // Check for invalid property IDs
                if (propertyId < 1 || propertyId >= getters.length) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Invalid property id %d in %s.getProperty\n",
                            propertyId, cls.getName());
                    return;
                }

                // Check for non-existing getter method
                if (getters[propertyId] == null) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "No getter method defined for property \"%s\" in %s\n",
                            propertyNames.get(propertyId), cls.getName());
                    return;
                }

                // Invoke the getter method
                Object output;
                try {
                    output = getters[propertyId].invoke(object);
                } catch (InvocationTargetException e) {
                    // Log exceptions thrown by the getter method
                    Throwable t = e.getTargetException();
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "%s.getProperty('%s'): %s\n",
                            cls.getName(), propertyNames.get(propertyId), t.toString());
                    return;
                } catch (IllegalAccessException e) {
                    // Tried to call a private method
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "IllegalAccessException calling %s.getProperty('%s')\n",
                            cls.getName(), propertyNames.get(propertyId));
                    return;
                }

                // Convert return value to GValue
                if (output != null)
                    ValueUtil.objectToValue(output, value);
            }, Arena.global());

            // Override the set_property virtual method
            gclass.overrideSetProperty((object, propertyId, value, pspec) -> {

                // Check for invalid property IDs
                if (propertyId < 1 || propertyId >= setters.length) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Invalid property id %d in %s.setProperty\n",
                            propertyId, cls.getName());
                    return;
                }

                // Check for non-existing getter method
                if (setters[propertyId] == null) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "No setter method defined for property \"%s\" in %s\n",
                            propertyNames.get(propertyId), cls.getName());
                    return;
                }

                // Convert argument to GValue
                Object input = ValueUtil.valueToObject(value);

                // Invoke the setter method
                if (input != null) {
                    try {
                        setters[propertyId].invoke(object, input);
                    } catch (InvocationTargetException e) {
                        // Log exceptions thrown by the setter method
                        Throwable t = e.getTargetException();
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "%s.setProperty('%s'): %s\n",
                                cls.getName(), propertyNames.get(propertyId), t.toString());
                    } catch (IllegalAccessException e) {
                        // Tried to call a private method
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "IllegalAccessException calling %s.setProperty('%s')\n",
                                cls.getName(), propertyNames.get(propertyId));
                    }
                }
            }, Arena.global());

            // Call g_class_install_properties with the generated ParamSpecs
            gclass.installProperties(pspecs);
        };
    }
}
