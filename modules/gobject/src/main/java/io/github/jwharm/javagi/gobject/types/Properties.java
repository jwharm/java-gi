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

package io.github.jwharm.javagi.gobject.types;

import io.github.jwharm.javagi.base.FunctionPointer;
import io.github.jwharm.javagi.gobject.InstanceCache;
import io.github.jwharm.javagi.gobject.annotations.Property;
import io.github.jwharm.javagi.base.Proxy;
import io.github.jwharm.javagi.base.ProxyInstance;
import io.github.jwharm.javagi.gobject.ValueUtil;
import io.github.jwharm.javagi.interop.Interop;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.gnome.gobject.*;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

import static io.github.jwharm.javagi.Constants.LOG_DOMAIN;
import static io.github.jwharm.javagi.gobject.annotations.Property.NOT_SET;
import static java.lang.Character.isUpperCase;

/**
 * Helper class to register properties in a new GType.
 */
public class Properties {

    /**
     * Read the value type from the GParamSpec of a GObject property.
     *
     * @param  objectClass  the GObject typeclass that has a property installed
     *                      with the provided name
     * @param  propertyName the name of the property
     * @return the value type of the GParamSpec of the GObject property, or null
     *         if not found
     */
    public static Type readPropertyValueType(GObject.ObjectClass objectClass,
                                             String propertyName) {
        ParamSpec pspec = objectClass.findProperty(propertyName);
        if (pspec == null) {
            throw new IllegalArgumentException("Cannot find property \"%s\" for type %s\n"
                    .formatted(
                            propertyName,
                            GObjects.typeName(objectClass.readGType())));
        }
        var pclass = (ParamSpec.ParamSpecClass) pspec.readGClass();
        return pclass == null ? null : pclass.readValueType();
    }

    /**
     * Set a property of an object.
     *
     * @param  propertyName  the name of the property to set
     * @param  propertyValue the new property propertyValue
     * @throws IllegalArgumentException if a property with this name is not
     *                                  found for the object
     */
    public static void setProperty(GObject gobject, String propertyName,
                                   Object propertyValue) {
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
    public static <T extends GObject>
    T newGObjectWithProperties(Type objectType,
                               Object... propertyNamesAndValues) {

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
                            // Ignore a closing null parameter (often expected
                            // by GObject vararg functions)
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
                 * Create and return the GObject with the property names and
                 * values.
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
        String value;
        if (methodName.startsWith("is"))
            value = methodName.substring(2);
        else if (methodName.startsWith("get") || methodName.startsWith("set"))
            value = methodName.substring(3);
        else
            throw new IllegalArgumentException(
                    "Cannot infer property name from method named " + methodName);
        return value.replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                .toLowerCase().replaceAll("\\.", "");
    }

    private static boolean isGetter(Method method) {
        if (skip(method))
            return false;

        if (method.getReturnType().equals(void.class)
                || method.getParameterCount() != 0)
            return false;

        String name = method.getName();

        if (name.startsWith("get")
                && name.length() > 3
                && isUpperCase(name.charAt(3)))
            return true;

        // Boolean getter can be either getFoo() or isFoo()
        return method.getReturnType().equals(boolean.class)
                && name.startsWith("is")
                && name.length() > 2
                && isUpperCase(name.charAt(2));
    }

    private static boolean isSetter(Method method) {
        if (skip(method))
            return false;

        return method.getReturnType().equals(void.class)
                && method.getParameterCount() == 1
                && method.getName().startsWith("set")
                && method.getName().length() > 3
                && isUpperCase(method.getName().charAt(3));
    }

    private static Class<?> getJavaType(Method method) {
        if (method.getReturnType().equals(void.class))
            return method.getParameterTypes()[0];
        else
            return method.getReturnType();
    }

    /*
     * Infer the ParamSpec class from the Java class that is used in the
     * getter/setter method.
     */
    private static Class<? extends ParamSpec> inferType(Class<?> type) {
        if (type.equals(boolean.class) || type.equals(Boolean.class))
            return ParamSpecBoolean.class;

        else if (type.equals(byte.class) || type.equals(Byte.class))
            return ParamSpecChar.class;

        else if (type.equals(char.class) || type.equals(Character.class))
            return ParamSpecChar.class;

        else if (type.equals(double.class) || type.equals(Double.class))
            return ParamSpecDouble.class;

        else if (type.equals(float.class) || type.equals(Float.class))
            return ParamSpecFloat.class;

        else if (type.equals(int.class) || type.equals(Integer.class))
            return ParamSpecInt.class;

        else if (type.equals(long.class) || type.equals(Long.class))
            return ParamSpecLong.class;

        else if (type.equals(String.class))
            return ParamSpecString.class;

        else if (Type.class.isAssignableFrom(type))
            return ParamSpecGType.class;

        // GObject class
        else if (GObject.class.isAssignableFrom(type))
            return ParamSpecObject.class;

        // Struct
        else if (ProxyInstance.class.isAssignableFrom(type))
            return ParamSpecBoxed.class;

        // GObject interface
        else if (Proxy.class.isAssignableFrom(type))
            return ParamSpecObject.class;

        throw new IllegalArgumentException("Invalid property type " + type.getSimpleName());
    }

    static void checkParameters(String property, String minimumValue,
                                String maximumValue, String defaultValue,
                                boolean defAllowed) {
        if (!NOT_SET.equals(minimumValue))
            throw new IllegalArgumentException(
                    "No minimum value allowed on property " + property);
        if (!NOT_SET.equals(maximumValue))
            throw new IllegalArgumentException(
                    "No maximum value allowed on property " + property);
        if (!defAllowed && !NOT_SET.equals(defaultValue))
            throw new IllegalArgumentException(
                    "No default value allowed on property " + property);
    }

    static boolean notSet(String s) {
        return NOT_SET.equals(s);
    }

    /*
     * Create a ParamSpec of the requested class.
     */
    private static ParamSpec createParamSpec(Class<? extends ParamSpec> pClass,
                                             String name,
                                             Set<ParamFlags> flags,
                                             String min, String max, String def) {
        if (pClass.equals(ParamSpecBoolean.class)) {
            checkParameters(name, min, max, def, true);
            var defVal = !notSet(def) && Boolean.parseBoolean(def);
            return GObjects.paramSpecBoolean(name, name, name, defVal, flags);
        }

        else if (pClass.equals(ParamSpecChar.class)) {
            var minVal = notSet(min) ? Byte.MIN_VALUE : Byte.parseByte(min);
            var maxVal = notSet(max) ? Byte.MAX_VALUE : Byte.parseByte(max);
            var defVal = notSet(def) ? (byte) 0 : Byte.parseByte(def);
            return GObjects.paramSpecChar(name, name, name,
                    minVal, maxVal, defVal, flags);
        }

        else if (pClass.equals(ParamSpecDouble.class)) {
            var minVal = notSet(min) ? -Double.MIN_VALUE : Double.parseDouble(min);
            var maxVal = notSet(max) ? Double.MAX_VALUE : Double.parseDouble(max);
            var defVal = notSet(def) ? 0.0d : Double.parseDouble(def);
            return GObjects.paramSpecDouble(name, name, name,
                    minVal, maxVal, defVal, flags);
        }

        else if (pClass.equals(ParamSpecFloat.class)) {
            var minVal = notSet(min) ? -Float.MIN_VALUE : Float.parseFloat(min);
            var maxVal = notSet(max) ? Float.MAX_VALUE : Float.parseFloat(max);
            var defVal = notSet(def) ? 0.0f : Float.parseFloat(def);
            return GObjects.paramSpecFloat(name, name, name,
                    minVal, maxVal, defVal, flags);
        }

        else if (pClass.equals(ParamSpecGType.class)) {
            checkParameters(name, min, max, def, true);
            var defVal = notSet(def) ? Types.NONE : GObjects.typeFromName(def);
            return GObjects.paramSpecGtype(name, name, name, defVal, flags);
        }

        else if (pClass.equals(ParamSpecInt.class)) {
            var minVal = notSet(min) ? Integer.MIN_VALUE : Integer.parseInt(min);
            var maxVal = notSet(max) ? Integer.MAX_VALUE : Integer.parseInt(max);
            var defVal = notSet(def) ? 0 : Integer.parseInt(def);
            return GObjects.paramSpecInt(name, name, name,
                    minVal, maxVal, defVal, flags);
        }

        else if (pClass.equals(ParamSpecInt64.class)) {
            var minVal = notSet(min) ? Long.MIN_VALUE : Long.parseLong(min);
            var maxVal = notSet(max) ? Long.MAX_VALUE : Long.parseLong(max);
            var defVal = notSet(def) ? 0L : Long.parseLong(def);
            return GObjects.paramSpecInt64(name, name, name,
                    minVal, maxVal, defVal, flags);
        }

        else if (pClass.equals(ParamSpecLong.class)) {
            var minVal = notSet(min) ? Integer.MIN_VALUE : Integer.parseInt(min);
            var maxVal = notSet(max) ? Integer.MAX_VALUE : Integer.parseInt(max);
            var defVal = notSet(def) ? 0 : Integer.parseInt(def);
            return GObjects.paramSpecLong(name, name, name,
                    minVal, maxVal, defVal, flags);
            }

        else if (pClass.equals(ParamSpecPointer.class)) {
            checkParameters(name, min, max, def, false);
            return GObjects.paramSpecPointer(name, name, name, flags);
        }

        else if (pClass.equals(ParamSpecString.class)) {
            checkParameters(name, min, max, def, true);
            var defVal = notSet(def) ? null : def;
            return GObjects.paramSpecString(name, name, name, defVal, flags);
        }

        else if (pClass.equals(ParamSpecUChar.class)) {
            var minVal = notSet(min) ? (byte) 0 : Byte.parseByte(min);
            var maxVal = notSet(max) ? Byte.MAX_VALUE : Byte.parseByte(max);
            var defVal = notSet(def) ? (byte) 0 : Byte.parseByte(def);
            return GObjects.paramSpecUchar(name, name, name,
                    minVal, maxVal, defVal, flags);
        }

        else if (pClass.equals(ParamSpecUInt.class)) {
            var minVal = notSet(min) ? 0 : Integer.parseInt(min);
            var maxVal = notSet(max) ? Integer.MAX_VALUE : Integer.parseInt(max);
            var defVal = notSet(def) ? 0 : Integer.parseInt(def);
            return GObjects.paramSpecUint(name, name, name,
                    minVal, maxVal, defVal, flags);
        }

        else if (pClass.equals(ParamSpecUInt64.class)) {
            var minVal = notSet(min) ? 0L : Long.parseLong(min);
            var maxVal = notSet(max) ? Long.MAX_VALUE : Long.parseLong(max);
            var defVal = notSet(def) ? 0L : Long.parseLong(def);
            return GObjects.paramSpecUint64(name, name, name,
                    minVal, maxVal, defVal, flags);
        }

        else if (pClass.equals(ParamSpecULong.class)) {
            var minVal = notSet(min) ? 0 : Integer.parseInt(min);
            var maxVal = notSet(max) ? Integer.MAX_VALUE : Integer.parseInt(max);
            var defVal = notSet(def) ? 0 : Integer.parseInt(def);
            return GObjects.paramSpecUlong(name, name, name,
                    minVal, maxVal, defVal, flags);
        }

        else if (pClass.equals(ParamSpecUnichar.class)) {
            checkParameters(name, min, max, def, true);
            var defVal = notSet(def) ? 0 : Integer.parseInt(def);
            return GObjects.paramSpecUnichar(name, name, name, defVal, flags);
        }

        throw new IllegalArgumentException(
                "Unsupported property type: " + pClass.getSimpleName());
    }

    /*
     * Create a GParamFlags based on {@code @Property} annotation parameters.
     */
    private static Set<ParamFlags> getFlags(Property property) {
        EnumSet<ParamFlags> flags = EnumSet.noneOf(ParamFlags.class);
        if (property.readable())       flags.add(ParamFlags.READABLE);
        if (property.writable())       flags.add(ParamFlags.WRITABLE);
        if (property.construct())      flags.add(ParamFlags.CONSTRUCT);
        if (property.constructOnly())  flags.add(ParamFlags.CONSTRUCT_ONLY);
        if (property.explicitNotify()) flags.add(ParamFlags.EXPLICIT_NOTIFY);
        if (property.deprecated())     flags.add(ParamFlags.DEPRECATED);
        return flags;
    }

    /*
     * Check if a `@Property` annotation with `skip=true`is set on this method
     */
    private static boolean skip(Method method) {
        return method.isAnnotationPresent(Property.class)
                && method.getAnnotation(Property.class).skip();
    }

    private final Map<Integer, String> names;
    private final Map<Integer, Method> getters;
    private final Map<Integer, Method> setters;
    private final Map<Integer, ParamSpec> paramSpecs;
    private int index = 0;

    public Properties() {
        names = new HashMap<>();
        getters = new HashMap<>();
        setters = new HashMap<>();
        paramSpecs = new HashMap<>();
    }

    /*
     * Find all property methods: `T getFooBar()`, `void setFooBar(T t)`, and
     * methods annotated with `@Property`. The results are put in the hashmaps.
     */
    private void inferProperties(Class<?> cls) {
        Map<String, Method> possibleGetters = new HashMap<>();

        // Methods with annotation @Property
        for (var method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Property.class)) {
                Property p = method.getAnnotation(Property.class);
                if (p.skip())
                    continue;

                String name = p.name();
                if (name.isBlank()) name = getPropertyName(method.getName());

                if (names.containsValue(name)) {
                    for (var entry : names.entrySet()) {
                        if (entry.getValue().equals(name)) {
                            int id = entry.getKey();
                            if (method.getReturnType().equals(void.class))
                                setters.put(id, method);
                            else
                                getters.put(id, method);
                            break;
                        }
                    }
                } else {
                    index++;
                    var flags = getFlags(p);
                    Class<?> javaType = getJavaType(method);
                    var paramSpecClass = inferType(javaType);
                    var paramSpec = createParamSpec(
                            paramSpecClass, name, flags,
                            p.minimumValue(), p.maximumValue(), p.defaultValue());
                    paramSpecs.put(index, paramSpec);
                    names.put(index, name);
                    if (method.getReturnType().equals(void.class))
                        setters.put(index, method);
                    else
                        getters.put(index, method);
                }
            }
        }

        // Getter methods (`T getFoo()` or `boolean isFoo()`)
        for (var method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Property.class))
                continue;

            if (isGetter(method))
                possibleGetters.put(getPropertyName(method.getName()), method);
        }

        // Setter methods (`void setFoo(T t)`) for which a corresponding
        // getter method was found
        for (var method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Property.class))
                continue;

            if (isSetter(method)) {
                String name = getPropertyName(method.getName());
                if (possibleGetters.containsKey(name)) {
                    Method getter = possibleGetters.get(name);

                    // Check that the getter and setter have the same type
                    if (!getJavaType(getter).equals(getJavaType(method)))
                        continue;

                    // Prevent multiple properties with the same name
                    if (names.containsValue(name))
                        continue;

                    index++;
                    names.put(index, name);
                    getters.put(index, getter);
                    setters.put(index, method);
                    Class<?> javaType = getJavaType(method);
                    Class<? extends ParamSpec> paramSpecClass = inferType(javaType);
                    Set<ParamFlags> flags = EnumSet.of(ParamFlags.READABLE, ParamFlags.WRITABLE);
                    ParamSpec paramSpec = createParamSpec(
                            paramSpecClass, name, flags, NOT_SET, NOT_SET, NOT_SET);
                    paramSpecs.put(index, paramSpec);
                }
            }
        }
    }

    /**
     * If the provided class defines {@code @Property}-annotated getter and/or
     * setter methods, this function will return a class initializer that
     * registers these properties as GObject properties and overrides the
     * {@code GObject.getProperty} and {@code setProperty} methods to call the
     * annotated getters and setters.
     *
     * @param  cls  the class that possibly contains @Property annotations
     * @return a class initializer that registers the properties
     */
    public Consumer<TypeClass> installProperties(Class<?> cls) {
        inferProperties(cls);
        if (index == 0)
            return null;

        // Return class initializer method that installs the properties.
        return (gclass) -> {
            // Override the get_property virtual method
            overrideGetProperty(gclass, (object, propertyId, value, _) -> {

                // Check for invalid property IDs
                if (propertyId < 1 || propertyId > index) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Invalid property id %d in %s.getProperty\n",
                            propertyId, cls.getSimpleName());
                    return;
                }

                String name = names.get(propertyId);
                if (name == null) name = "";
                Method getter = getters.get(propertyId);

                if (getter == null) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "No getter method defined for property with ID=%d and name='%s' in %s\n",
                            propertyId, name, cls.getSimpleName());
                    return;
                }

                // Invoke the getter method
                Object output;
                try {
                    output = getter.invoke(object);
                } catch (InvocationTargetException e) {
                    // Log exceptions thrown by the getter method
                    Throwable t = e.getTargetException();
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "%s.getProperty('%s'): %s\n",
                            cls.getSimpleName(), name, t.toString());
                    return;
                } catch (IllegalAccessException e) {
                    // Tried to call a private method
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "IllegalAccessException calling %s.getProperty('%s')\n",
                            cls.getSimpleName(), name);
                    return;
                }

                // Convert return value to GValue
                if (output != null)
                    ValueUtil.objectToValue(output, value);
            }, Arena.global());

            // Override the set_property virtual method
            overrideSetProperty(gclass, (object, propertyId, value, _) -> {

                // Check for invalid property IDs
                if (propertyId < 1 || propertyId > index) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Invalid property id %d in %s.setProperty\n",
                            propertyId, cls.getSimpleName());
                    return;
                }

                String name = names.get(propertyId);
                if (name == null) name = "";
                Method setter = setters.get(propertyId);

                if (setter == null) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "No getter method defined for property with ID=%d and name='%s' in %s\n",
                            propertyId, name, cls.getSimpleName());
                    return;
                }

                // Convert argument to GValue
                Object input = ValueUtil.valueToObject(value);

                // Invoke the setter method
                if (input != null) {
                    try {
                        setter.invoke(object, input);
                    } catch (InvocationTargetException e) {
                        // Log exceptions thrown by the setter method
                        Throwable t = e.getTargetException();
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "%s.setProperty('%s'): %s\n",
                                cls.getSimpleName(), name, t.toString());
                    } catch (IllegalAccessException e) {
                        // Tried to call a private method
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "IllegalAccessException calling %s.setProperty('%s')\n",
                                cls.getSimpleName(), name);
                    }
                }
            }, Arena.global());

            // Register properties for the generated ParamSpecs
            if (gclass instanceof GObject.ObjectClass oclass) {
                for (int i = 1; i <= index; i++) {
                    oclass.installProperty(i, paramSpecs.get(i));
                }
            }

            else if (cls.isInterface() && Types.isGObjectBased(cls)) {
                var giface = new TypeInterface(gclass.handle());
                for (int i = 1; i <= index; i++) {
                    GObject.interfaceInstallProperty(giface, paramSpecs.get(i));
                }
            }

            else {
                GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                        "Class or interface %s is not based on GObject\n",
                        cls.getSimpleName());
            }
        };
    }

    private static void overrideGetProperty(Proxy instance, GetPropertyCallback getProperty, Arena _arena) {
        GObject.ObjectClass.getMemoryLayout().varHandle(MemoryLayout.PathElement.groupElement("get_property"))
                .set(instance.handle(), 0, (getProperty == null ? MemorySegment.NULL : getProperty.toCallback(_arena)));
    }

    private static void overrideSetProperty(Proxy instance, SetPropertyCallback setProperty, Arena _arena) {
        GObject.ObjectClass.getMemoryLayout().varHandle(MemoryLayout.PathElement.groupElement("set_property"))
                .set(instance.handle(), 0, (setProperty == null ? MemorySegment.NULL : setProperty.toCallback(_arena)));
    }

    /**
     * Functional interface declaration of the {@code GetPropertyCallback} callback.
     */
    @FunctionalInterface
    public interface GetPropertyCallback extends FunctionPointer {
        void run(GObject object, int propertyId, Value value, ParamSpec pspec);

        default void upcall(MemorySegment object, int propertyId, MemorySegment value, MemorySegment pspec) {
            run((GObject) InstanceCache.getForType(object, GObject::new, false),
                    propertyId,
                    MemorySegment.NULL.equals(value) ? null : new Value(value),
                    (ParamSpec) InstanceCache.getForType(pspec, ParamSpec.ParamSpecImpl::new, false));
        }

        default MemorySegment toCallback(Arena arena) {
            FunctionDescriptor _fdesc = FunctionDescriptor.ofVoid(
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS);
            MethodHandle _handle = Interop.upcallHandle(MethodHandles.lookup(),GetPropertyCallback.class, _fdesc);
            return Linker.nativeLinker().upcallStub(_handle.bindTo(this), _fdesc, arena);
        }
    }

    /**
     * Functional interface declaration of the {@code SetPropertyCallback} callback.
     */
    @FunctionalInterface
    public interface SetPropertyCallback extends FunctionPointer {
        void run(GObject object, int propertyId, Value value, ParamSpec pspec);

        default void upcall(MemorySegment object, int propertyId, MemorySegment value, MemorySegment pspec) {
            run((GObject) InstanceCache.getForType(object, GObject::new, false),
                    propertyId,
                    MemorySegment.NULL.equals(value) ? null : new Value(value),
                    (ParamSpec) InstanceCache.getForType(pspec, ParamSpec.ParamSpecImpl::new, false));
        }

        default MemorySegment toCallback(Arena arena) {
            FunctionDescriptor _fdesc = FunctionDescriptor.ofVoid(
                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS);
            MethodHandle _handle = Interop.upcallHandle(MethodHandles.lookup(), SetPropertyCallback.class, _fdesc);
            return Linker.nativeLinker().upcallStub(_handle.bindTo(this), _fdesc, arena);
        }
    }
}
