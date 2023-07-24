package io.github.jwharm.javagi.types;

import io.github.jwharm.javagi.base.Proxy;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.gobject.GObject;
import org.gnome.gobject.TypeInterface;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static io.github.jwharm.javagi.Constants.LOG_DOMAIN;

/**
 * Helper class to register method overrides in a new GType
 */
public class Overrides {

    /**
     * Find declared methods that override methods defined in a GObject type class,
     * and return a class initializer lambda that will register the method overrides
     * in the class virtual function table.
     * @param cls the class that possibly declares method overrides
     * @return a lambda to run during class initialization that will register the virtual functions
     * @param <T> the class must extend {@link GObject}
     * @param <TC> the returned lambda expects a {@link GObject.ObjectClass} parameter
     */
    public static <T extends GObject, TC extends GObject.ObjectClass> Consumer<TC> overrideClassMethods(Class<T> cls) {
        Class<?> typeStruct = Types.getTypeClass(cls);
        if (typeStruct == null) {
            return null;
        }
        Class<?> parentClass = cls.getSuperclass();

        // Find all overridden methods
        List<Method> methods = new ArrayList<>();
        for (Method method : cls.getDeclaredMethods()) {
            try {
                Method virtual = parentClass.getMethod(method.getName(), method.getParameterTypes());
                if (! Proxy.class.isAssignableFrom(virtual.getDeclaringClass())) {
                    continue;
                }
            } catch (NoSuchMethodException e) {
                continue;
            }
            String name = method.getName();
            name = "override" + name.substring(0, 1).toUpperCase() + name.substring(1);
            try {
                typeStruct.getMethod(name, Method.class);
            } catch (NoSuchMethodException e) {
                continue;
            }
            methods.add(method);
        }
        if (methods.isEmpty()) {
            return null;
        }

        // Register the overridden methods in the typeclass
        return (gclass) -> {
            for (Method method : methods) {
                String name = method.getName();
                name = "override" + name.substring(0, 1).toUpperCase() + name.substring(1);
                try {
                    Method overrider = gclass.getClass().getMethod(name, Method.class);
                    overrider.invoke(gclass, method);
                } catch (InvocationTargetException ite) {
                    System.err.printf("Cannot override method %s in class %s: %s\n",
                            method.getName(), cls.getName(), ite.getTargetException().toString());
                    ite.printStackTrace();
                } catch (Exception e) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Cannot override method %s in class %s: %s\n",
                            method.getName(), cls.getName(), e.toString());
                }
            }
        };
    }

    /**
     * Find declared methods that implement methods defined in the provided GObject interface,
     * and return an interface initializer lambda that will register the method overrides
     * in the interface virtual function table.
     * @param cls the class that possibly declares method overrides
     * @param iface the interface from which methods are implemented
     * @return a lambda to run during interface initialization that will register the virtual functions
     * @param <T> the class must extend {@link GObject}
     * @param <TI> the returned lambda expects a {@link TypeInterface} parameter
     */
    static <T extends GObject, TI extends TypeInterface> Consumer<TI> overrideInterfaceMethods(Class<T> cls, Class<?> iface) {
        // Find all overridden methods
        Class<TI> typeStruct = Types.getTypeInterface(iface);
        if (typeStruct == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find TypeInterface class for interface %s\n", iface);
            return null;
        }
        var constructor = Types.getAddressConstructor(typeStruct);
        if (constructor == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find constructor in TypeInterface %s\n", typeStruct);
            return null;
        }

        // Find all overridden methods
        List<Method> methods = new ArrayList<>();
        for (Method method : cls.getDeclaredMethods()) {
            try {
                iface.getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                continue;
            }
            String name = method.getName();
            name = "override" + name.substring(0, 1).toUpperCase() + name.substring(1);
            try {
                typeStruct.getMethod(name, Method.class);
            } catch (NoSuchMethodException e) {
                continue;
            }
            methods.add(method);
        }
        if (methods.isEmpty()) {
            return null;
        }

        // Register the overridden methods in the typeinterface
        return (giface) -> {
            for (Method method : methods) {
                String name = method.getName();
                name = "override" + name.substring(0, 1).toUpperCase() + name.substring(1);
                try {
                    TI ifaceInstance = constructor.apply(giface.handle()); // upcast to the actual type
                    Method overrider = typeStruct.getMethod(name, Method.class);
                    overrider.invoke(ifaceInstance, method);
                } catch (Exception e) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Cannot override method %s from interface %s in class %s: %s\n",
                            method.getName(), iface.getName(), cls.getName(), e.toString());
                }
            }
        };
    }
}
