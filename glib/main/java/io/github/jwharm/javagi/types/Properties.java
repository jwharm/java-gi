package io.github.jwharm.javagi.types;

import io.github.jwharm.javagi.annotations.Property;
import io.github.jwharm.javagi.util.ValueUtil;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.gnome.gobject.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static io.github.jwharm.javagi.types.Types.LOG_DOMAIN;

/**
 * Helper class to register properties in a new GType
 */
public class Properties {

    // Create a GParamFlags based on @Property annotation parameters
    private static ParamFlags getFlags(Property property) {
        ParamFlags flags = new ParamFlags(0);
        if (property.readable()) flags = flags.or(ParamFlags.READABLE);
        if (property.writable()) flags = flags.or(ParamFlags.WRITABLE);
        if (property.construct()) flags = flags.or(ParamFlags.CONSTRUCT);
        if (property.constructOnly()) flags = flags.or(ParamFlags.CONSTRUCT_ONLY);
        if (property.explicitNotify()) flags = flags.or(ParamFlags.EXPLICIT_NOTIFY);
        if (property.deprecated()) flags = flags.or(ParamFlags.DEPRECATED);
        return flags;
    }

    /**
     * If the provided class defines @Property-annotated getter and/or setter methods,
     * this function will return a class initializer that registers these properties as
     * a GObject properties and overrides the {@code GObject.getProperty} and
     * {@code setProperty} methods to call the annotated getters and setters.
     * @param cls the class that possibly contains @Property annotations
     * @return a class initalizer that registers the properties
     * @param <T> the class must extend {@link org.gnome.gobject.GObject}
     * @param <TC> the returned lambda expects a {@link GObject.ObjectClass} parameter
     */
    public static <T extends GObject, TC extends GObject.ObjectClass> Consumer<TC> installProperties(Class<T> cls) {
        List<ParamSpec> propertySpecs = new ArrayList<>();
        propertySpecs.add(null); // Index 0 is reserved

        // Create an index of property names.
        // The list is used to obtain a property id using `list.indexOf(property.name())`
        List<String> propertyNames = new ArrayList<>();
        propertyNames.add(null); // index 0 is reserved

        for (Method method : cls.getDeclaredMethods()) {

            // Look for methods with annotation @Property
            Property p = method.getAnnotation(Property.class);
            if (p == null) {
                continue;
            }

            // Check if the type is set on this method. It can be set on either the getter or setter.
            // If the type is not set, it defaults to ParamSpec.class
            if (p.type().equals(ParamSpec.class)) {
                continue;
            }

            ParamSpec ps;
            if (p.type().equals(ParamSpecBoolean.class)) {
                ps = GObjects.paramSpecBoolean(p.name(), p.name(), p.name(), false, getFlags(p));
            } else if (p.type().equals(ParamSpecChar.class)) {
                ps = GObjects.paramSpecChar(p.name(), p.name(), p.name(), Byte.MIN_VALUE, Byte.MAX_VALUE, (byte) 0, getFlags(p));
            } else if (p.type().equals(ParamSpecDouble.class)) {
                ps = GObjects.paramSpecDouble(p.name(), p.name(), p.name(), Double.MIN_VALUE, Double.MAX_VALUE, 0, getFlags(p));
            } else if (p.type().equals(ParamSpecFloat.class)) {
                ps = GObjects.paramSpecFloat(p.name(), p.name(), p.name(), Float.MIN_VALUE, Float.MAX_VALUE, 0, getFlags(p));
            } else if (p.type().equals(ParamSpecGType.class)) {
                ps = GObjects.paramSpecGtype(p.name(), p.name(), p.name(), Types.G_TYPE_NONE, getFlags(p));
            } else if (p.type().equals(ParamSpecInt.class)) {
                ps = GObjects.paramSpecInt(p.name(), p.name(), p.name(), Integer.MIN_VALUE, Integer.MAX_VALUE, 0, getFlags(p));
            } else if (p.type().equals(ParamSpecInt64.class)) {
                ps = GObjects.paramSpecInt64(p.name(), p.name(), p.name(), Long.MIN_VALUE, Long.MAX_VALUE, 0, getFlags(p));
            } else if (p.type().equals(ParamSpecLong.class)) {
                ps = GObjects.paramSpecLong(p.name(), p.name(), p.name(), Integer.MIN_VALUE, Integer.MAX_VALUE, 0, getFlags(p));
            } else if (p.type().equals(ParamSpecPointer.class)) {
                ps = GObjects.paramSpecPointer(p.name(), p.name(), p.name(), getFlags(p));
            } else if (p.type().equals(ParamSpecString.class)) {
                ps = GObjects.paramSpecString(p.name(), p.name(), p.name(), null, getFlags(p));
            } else if (p.type().equals(ParamSpecUChar.class)) {
                ps = GObjects.paramSpecUchar(p.name(), p.name(), p.name(), (byte) 0, Byte.MAX_VALUE, (byte) 0, getFlags(p));
            } else if (p.type().equals(ParamSpecUInt.class)) {
                ps = GObjects.paramSpecUint(p.name(), p.name(), p.name(), 0, Integer.MAX_VALUE, 0, getFlags(p));
            } else if (p.type().equals(ParamSpecUInt64.class)) {
                ps = GObjects.paramSpecUint64(p.name(), p.name(), p.name(), 0, Long.MAX_VALUE, 0, getFlags(p));
            } else if (p.type().equals(ParamSpecULong.class)) {
                ps = GObjects.paramSpecUlong(p.name(), p.name(), p.name(), 0, Integer.MAX_VALUE, 0, getFlags(p));
            } else if (p.type().equals(ParamSpecUnichar.class)) {
                ps = GObjects.paramSpecUnichar(p.name(), p.name(), p.name(), 0, getFlags(p));
            } else {
                GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                        "Unsupported ParamSpec %s in class %s:\n",
                        p.type().getName(), cls.getName());
                return null;
            }
            propertySpecs.add(ps);
            propertyNames.add(p.name());
        }

        // No properties found?
        if (propertySpecs.size() == 1) {
            return null;
        }

        // Create arrays of getter and setter methods.
        Method[] getters = new Method[propertySpecs.size()];
        Method[] setters = new Method[propertySpecs.size()];

        for (Method method : cls.getDeclaredMethods()) {
            if (! method.isAnnotationPresent(Property.class)) {
                continue;
            }
            Property property = method.getDeclaredAnnotation(Property.class);
            int idx = propertyNames.indexOf(property.name());

            // Returns void -> setter, else -> getter
            if (method.getReturnType().equals(void.class)) {
                setters[idx] = method;
            } else {
                getters[idx] = method;
            }
        }

        // Create GParamSpec array. Index 0 is reserved.
        final ParamSpec[] pspecs = new ParamSpec[propertySpecs.size()];
        for (int i = 1; i < propertySpecs.size(); i++) {
            pspecs[i] = propertySpecs.get(i);
        }
        // Return class initializer method that installs the properties.
        return (gclass) -> {
            gclass.overrideGetProperty((object, propertyId, value, pspec) -> {
                if (propertyId < 1 || propertyId >= getters.length) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Invalid property id %d in %s.getProperty:\n",
                            propertyId, cls.getName());
                    return;
                }
                if (getters[propertyId] == null) {
                    return;
                }
                Object output;
                try {
                    output = getters[propertyId].invoke(object);
                } catch (InvocationTargetException e) {
                    Throwable t = e.getTargetException();
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Exception %s thrown by %s.getProperty('%s'):\n",
                            t.toString(), cls.getName(), propertyNames.get(propertyId));
                    return;
                } catch (IllegalAccessException e) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Exception %s in %s.getProperty('%s'):\n",
                            e.toString(), cls.getName(), propertyNames.get(propertyId));
                    return;
                }
                if (output != null) {
                    ValueUtil.objectToValue(output, value, pspec);
                }
            });

            gclass.overrideSetProperty((object, propertyId, value, pspec) -> {
                if (propertyId < 1 || propertyId >= setters.length) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Invalid property id %d in %s.setProperty:\n",
                            propertyId, cls.getName());
                    return;
                }
                if (setters[propertyId] == null) {
                    return;
                }
                Object input = ValueUtil.valueToObject(value, pspec);
                if (input != null) {
                    try {
                        setters[propertyId].invoke(object, input);
                    } catch (InvocationTargetException e) {
                        Throwable t = e.getTargetException();
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Exception %s thrown by %s.setProperty('%s'):\n",
                                t.toString(), cls.getName(), propertyNames.get(propertyId));
                    } catch (IllegalAccessException e) {
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Exception %s in %s.setProperty('%s'):\n",
                                e.toString(), cls.getName(), propertyNames.get(propertyId));
                    }
                }
            });
            gclass.installProperties(pspecs);
        };
    }
}
