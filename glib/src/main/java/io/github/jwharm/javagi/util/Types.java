package io.github.jwharm.javagi.util;

import io.github.jwharm.javagi.annotations.ClassInit;
import io.github.jwharm.javagi.annotations.CustomType;
import io.github.jwharm.javagi.annotations.InstanceInit;
import io.github.jwharm.javagi.annotations.InterfaceInit;
import io.github.jwharm.javagi.base.Proxy;
import io.github.jwharm.javagi.interop.InstanceCache;
import io.github.jwharm.javagi.interop.TypeCache;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.gnome.gobject.*;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryLayout;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class Types {

    private static final String LOG_DOMAIN = "java-gi";

    public static String getName(Class<?> cls) {
        // Default type name: fully qualified Java class name
        String typeNameInput = cls.getName();

        // Check for an annotation that overrides the type name
        if (cls.isAnnotationPresent(CustomType.class)) {
            var annotation = cls.getAnnotation(CustomType.class);
            if (! "".equals(annotation.name())) {
                typeNameInput = annotation.name();
            }
        }

        // Replace all characters except a-z or A-Z with underscores
        return typeNameInput.replaceAll("[^a-zA-Z]", "_");
    }

    public static <T extends GObject> MemoryLayout getInstanceLayout(Class<T> cls, String typeName) {
            // Get instance-memorylayout of this class
            MemoryLayout instanceLayout = getLayout(cls);
            if (instanceLayout != null)
                return instanceLayout;

            // If no memory layout was defined, create a default memory layout
            // that only has a pointer to the parent class' memory layout.
            MemoryLayout parentLayout = getLayout(cls.getSuperclass());

            if (parentLayout == null) {
                GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                        "Cannot find memory layout definition for class %s\n", cls.getName());
                return null;
            }

            return MemoryLayout.structLayout(
                    parentLayout.withName("parent_instance")
            ).withName(typeName);

    }

    @SuppressWarnings("unchecked")
    public static <T extends GObject, TC extends TypeClass> Class<TC> getTypeClass(Class<T> cls) {
        // Get the type-struct. This is an inner class that extends TypeClass.
        // If the type-struct is unavailable, get it from the parent class.
        Class<TC> typeClass = null;
        for (Class<?> gclass : cls.getClasses()) {
            if (TypeClass.class.isAssignableFrom(gclass)) {
                return (Class<TC>) gclass;
            }
        }
        for (Class<?> gclass : cls.getSuperclass().getClasses()) {
            if (TypeClass.class.isAssignableFrom(gclass)) {
                return (Class<TC>) gclass;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <TI extends TypeInterface> Class<TI> getTypeInterface(Class<?> iface) {
        // Get the type-struct. This is an inner class that extends TypeInterface.
        Class<TI> typeStruct = null;
        for (Class<?> giface : iface.getClasses()) {
            if (TypeInterface.class.isAssignableFrom(giface)) {
                return (Class<TI>) giface;
            }
        }
        return null;
    }

    /**
     * Generate a MemoryLayout struct with one member: the memorylayout of the parent's TypeClass
     */
    public static <T extends GObject> MemoryLayout getClassLayout(Class<T> cls, String typeName) {
        // Get the type-struct. This is an inner class that extends TypeClass.
        // If the type-struct is unavailable, get it from the parent class.
        Class<?> typeClass = getTypeClass(cls);
        if (typeClass == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find TypeClass for class %s\n", cls.getName());
            return null;
        }

        // Get class-memorylayout
        MemoryLayout parentLayout = getLayout(typeClass);

        if (parentLayout == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find class memory layout definition for class %s\n", cls.getName());
            return null;
        }

        return MemoryLayout.structLayout(
                parentLayout.withName("parent_class")
        ).withName(typeName + "Class");
    }

    public static Type getGType(Class<?> cls) {
        try {
            // invoke getType() on the class
            Method getTypeMethod = cls.getDeclaredMethod("getType");
            return (Type) getTypeMethod.invoke(null);

        } catch (Exception e) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot get GType for class %s: %s\n",
                    cls == null ? "null" : cls.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * Get the MemoryLayout that is returned by invoking {@code cls.getMemoryLayout()}
     */
    public static MemoryLayout getLayout(Class<?> cls) {
        try {
            // invoke getMemoryLayout() on the class
            Method getLayoutMethod = cls.getDeclaredMethod("getMemoryLayout");
            return (MemoryLayout) getLayoutMethod.invoke(null);

        } catch (Exception notfound) {
            return null;
        }
    }

    public static <T extends Proxy> Function<Addressable, T> getAddressConstructor(Class<T> cls) {
        Constructor<T> ctor;
        try {
            // Get memory address constructor
            ctor = cls.getConstructor(Addressable.class);
        } catch (NoSuchMethodException e) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find memory-address constructor definition for class %s: %s\n",
                    cls.getName(), e.getMessage());
            return null;
        }

        // Create a wrapper function that will run the constructor and catch exceptions
        return (addr) -> {
            try {
                return ctor.newInstance(addr);
            } catch (Exception e) {
                GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                        "Exception in constructor for class %s: %s\n",
                        cls.getName(), e.getMessage());
                return null;
            }
        };
    }

    public static <T extends GObject> Consumer<T> getInstanceInit(Class<T> cls) {
        // Find instance initializer function
        for (Method method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(InstanceInit.class)) {
                // Create a wrapper function that calls the instance initializer and logs exceptions
                return (inst) -> {
                    try {
                        method.invoke(inst);
                    } catch (Exception e) {
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Exception in %s instance init: %s\n", cls.getName(), e.getMessage());
                    }
                };
            }
        }
        return null;
    }

    public static <T extends GObject, TC extends TypeClass> Consumer<TC> getClassInit(Class<T> cls) {
        // Find class initializer function
        for (Method method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ClassInit.class)) {
                // Create a wrapper function that calls the class initializer and logs exceptions
                return (gclass) -> {
                    try {
                        method.invoke(null, gclass);
                    } catch (Exception e) {
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Exception in %s class init: %s\n", cls.getName(), e.getMessage());
                    }
                };
            }
        }
        return null;
    }

    public static <T extends GObject, TI extends TypeInterface> Consumer<TI> getInterfaceInit(Class<T> cls, Class<?> iface) {
        // Find all overridden methods
        Class<TI> typeStruct = getTypeInterface(iface);
        if (typeStruct == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find TypeInterface class for interface %s\n", iface);
            return null;
        }
        var constructor = getAddressConstructor(typeStruct);
        if (constructor == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find constructor in TypeInterface %s\n", typeStruct);
            return null;
        }

        // Find interface initializer function
        for (Method method : cls.getDeclaredMethods()) {
            if (! method.isAnnotationPresent(InterfaceInit.class)) {
                continue;
            }
            if (! (method.getParameterTypes().length == 1)) {
                continue;
            }
            Class<?> param = method.getParameterTypes()[0];
            if (! param.equals(typeStruct)) {
                continue;
            }
            // Create a wrapper function that calls the interface initializer and logs exceptions
            return (giface) -> {
                try {
                    TI ifaceInstance = constructor.apply(giface.handle());
                    method.invoke(null, ifaceInstance);
                } catch (Exception e) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Exception in %s interface init: %s\n", cls.getName(), e.getMessage());
                }
            };
        }
        return null;
    }

    public static <T extends GObject, TC extends TypeClass> Consumer<TC> overrideClassMethods(Class<T> cls) {
        Class<?> typeStruct = getTypeClass(cls);
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
                Method overrider = typeStruct.getMethod(name, Method.class);
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
                } catch (Exception e) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Cannot override method %s in class %s: %s\n",
                            method.getName(), cls.getName(), e.getMessage());
                }
            }
        };
    }

    public static <T extends GObject, TI extends TypeInterface> Consumer<TI> overrideInterfaceMethods(Class<T> cls, Class<?> iface) {
        // Find all overridden methods
        Class<TI> typeStruct = getTypeInterface(iface);
        if (typeStruct == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find TypeInterface class for interface %s\n", iface);
            return null;
        }
        var constructor = getAddressConstructor(typeStruct);
        if (constructor == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find constructor in TypeInterface %s\n", typeStruct);
            return null;
        }

        // Find all overridden methods
        List<Method> methods = new ArrayList<>();
        for (Method method : cls.getDeclaredMethods()) {
            try {
                Method virtual = iface.getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                continue;
            }
            String name = method.getName();
            name = "override" + name.substring(0, 1).toUpperCase() + name.substring(1);
            try {
                Method overrider = typeStruct.getMethod(name, Method.class);
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
                            method.getName(), iface.getName(), cls.getName(), e.getMessage());
                }
            }
        };
    }

    public static TypeFlags getTypeFlags(Class<?> cls) {
        // Set type flags
        TypeFlags flags = TypeFlags.NONE;
        if (Modifier.isAbstract(cls.getModifiers())) {
            flags = flags.or(TypeFlags.ABSTRACT);
        }
        if (Modifier.isFinal(cls.getModifiers())) {
            flags = flags.or(TypeFlags.FINAL);
        }
        return flags;
    }

    /**
     * Register a new GType for a Java class. The GType will inherit from the GType of the Java
     * superclass (using {@link Class#getSuperclass()}, and invoking {@code getType()} and
     * {@code getMemoryLayout()} using reflection).
     * <p>
     * The name of the new GType will be the simple name of the Java class, but can also be
     * specified with the {@link CustomType} annotation. (All invalid characters, including '.',
     * are replaced with underscores.)
     * <p>
     * Use {@link ClassInit} and {@link InstanceInit} annotations on static methods
     * in the Java class to indicate that these are to be called during GObject class- and
     * instance initialization.
     * <p>
     * The {@link TypeFlags#ABSTRACT} and {@link TypeFlags#FINAL} flags are set for abstract and
     * final Java classes.
     * @return the new registered GType
     * @param <T> The class must be derived from GObject
     */
    public static <T extends GObject, TC extends TypeClass> Type register(Class<T> cls) {
        if (cls == null) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Class is null\n");
            return null;
        }

        try {
            Class<?> parentClass = cls.getSuperclass();
            Type parentType = getGType(parentClass);
            String typeName = getName(cls);
            MemoryLayout classLayout = getClassLayout(cls, typeName);
            Consumer<TC> overridesInit = overrideClassMethods(cls);
            Consumer<TC> classInit = getClassInit(cls);
            MemoryLayout instanceLayout = getInstanceLayout(cls, typeName);
            Consumer<T> instanceInit = getInstanceInit(cls);
            Function<Addressable, T> constructor = getAddressConstructor(cls);
            TypeFlags flags = getTypeFlags(cls);

            if (parentType == null || classLayout == null || instanceLayout == null
                    || constructor == null || flags == null) {
                GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                        "Cannot register type %s\n", cls.getName());
                return null;
            }

            // Generate default init function
            if (instanceInit == null) instanceInit = $ -> {};

            // Override virtual methods before running a user-defined class init
            if (classInit != null && overridesInit != null)
                classInit = overridesInit.andThen(classInit);
            else if (classInit == null && overridesInit != null)
                classInit = overridesInit;
            else if (classInit == null)
                classInit = $ -> {}; // default class init function

            // Register the GType
            Type type = register(
                    parentType,
                    typeName,
                    classLayout,
                    classInit,
                    instanceLayout,
                    instanceInit,
                    constructor,
                    flags
            );

            // Add interfaces
            for (Class<?> iface : cls.getInterfaces()) {
                if (Proxy.class.isAssignableFrom(iface)) {
                    Type ifaceType = getGType(iface);
                    if (ifaceType == null) {
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Cannot implement interface %s on class %s: No GType\n",
                                iface.getName(), cls.getName());
                        continue;
                    }

                    InterfaceInfo interfaceInfo = InterfaceInfo.allocate();
                    Consumer<TypeInterface> ifaceOverridesInit = overrideInterfaceMethods(cls, iface);
                    Consumer<TypeInterface> ifaceInit = getInterfaceInit(cls, iface);

                    // Override virtual methods before running a user-defined interface init
                    if (ifaceInit != null && ifaceOverridesInit != null)
                        ifaceInit = ifaceOverridesInit.andThen(ifaceInit);
                    else if (ifaceInit == null && ifaceOverridesInit != null)
                        ifaceInit = ifaceOverridesInit;
                    else if (ifaceInit == null)
                        ifaceInit = $ -> {}; // default interface init function

                    Consumer<TypeInterface> finalIfaceInit = ifaceInit;
                    interfaceInfo.writeInterfaceInit((ti, data) -> finalIfaceInit.accept(ti));
                    GObjects.typeAddInterfaceStatic(type, ifaceType, interfaceInfo);
                }
            }

            return type;

        } catch (Exception e) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot register type %s: %s\n", cls.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * Register a new GType.
     * @param parentType Parent GType
     * @param typeName name of the GType
     * @param classLayout memory layout of the typeclass
     * @param classInit static class initializer function
     * @param instanceLayout memmory layout of the typeinstance
     * @param instanceInit static instance initializer function
     * @param constructor memory-address constructor
     * @param flags type flags
     * @return the new GType
     * @param <T>  the instance initializer function must accept the
     *             result of the memory address constructor
     * @param <TC> the class initializer function must accept a
     *            parameter that is a subclass of TypeClass
     */
    public static <T extends GObject, TC extends TypeClass> Type register(
            org.gnome.glib.Type parentType,
            String typeName,
            MemoryLayout classLayout,
            Consumer<TC> classInit,
            MemoryLayout instanceLayout,
            Consumer<T> instanceInit,
            Function<Addressable, T> constructor,
            TypeFlags flags
    ) {
        @SuppressWarnings("unchecked")
        Type type = GObjects.typeRegisterStaticSimple(
                parentType,
                typeName,
                (short) classLayout.byteSize(),
                // The data parameter is not used.
                (typeClass, data) -> classInit.accept((TC) typeClass),
                (short) instanceLayout.byteSize(),
                // The instance parameter is a type-instance of T, so construct a T proxy instance.
                // The typeClass parameter is not used.
                (instance, typeClass) -> {
                    // The instance is initially cached as TypeInstance.
                    // Overwrite it with a new T instance, and run init().
                    T newInstance = constructor.apply(instance.handle());
                    InstanceCache.put(newInstance.handle(), newInstance);
                    instanceInit.accept(newInstance);
                },
                flags
        );
        // Register the type and constructor in the cache
        TypeCache.register(type, constructor);
        return type;
    }
}
