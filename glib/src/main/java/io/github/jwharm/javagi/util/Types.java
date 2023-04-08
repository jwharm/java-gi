package io.github.jwharm.javagi.util;

import io.github.jwharm.javagi.annotations.*;
import io.github.jwharm.javagi.base.Proxy;
import io.github.jwharm.javagi.interop.InstanceCache;
import io.github.jwharm.javagi.interop.TypeCache;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.gnome.gobject.*;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemoryLayout;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
        if (cls.isAnnotationPresent(RegisteredType.class)) {
            var annotation = cls.getAnnotation(RegisteredType.class);
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
    public static <T extends GObject, TC extends GObject.ObjectClass> Class<TC> getTypeClass(Class<T> cls) {
        // Get the type-struct. This is an inner class that extends ObjectClass.
        // If the type-struct is unavailable, get it from the parent class.
        for (Class<?> gclass : cls.getDeclaredClasses()) {
            if (TypeClass.class.isAssignableFrom(gclass)) {
                return (Class<TC>) gclass;
            }
        }
        for (Class<?> gclass : cls.getSuperclass().getDeclaredClasses()) {
            if (TypeClass.class.isAssignableFrom(gclass)) {
                return (Class<TC>) gclass;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <TI extends TypeInterface> Class<TI> getTypeInterface(Class<?> iface) {
        // Get the type-struct. This is an inner class that extends TypeInterface.
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
        // Get the type-struct. This is an inner class that extends GObject.ObjectClass.
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
                    cls == null ? "null" : cls.getName(), e.toString());
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

    public static <T extends Proxy> Function<MemorySegment, T> getAddressConstructor(Class<T> cls) {
        Constructor<T> ctor;
        try {
            // Get memory address constructor
            ctor = cls.getConstructor(MemorySegment.class);
        } catch (NoSuchMethodException e) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find memory-address constructor definition for class %s: %s\n",
                    cls.getName(), e.toString());
            return null;
        }

        // Create a wrapper function that will run the constructor and catch exceptions
        return (addr) -> {
            try {
                return ctor.newInstance(addr);
            } catch (Exception e) {
                GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                        "Exception in constructor for class %s: %s\n",
                        cls.getName(), e.toString());
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
                                "Exception in %s instance init: %s\n", cls.getName(), e.toString());
                    }
                };
            }
        }
        return null;
    }

    public static <T extends GObject, TC extends GObject.ObjectClass> Consumer<TC> getClassInit(Class<T> cls) {
        // Find class initializer function
        for (Method method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ClassInit.class)) {
                // Create a wrapper function that calls the class initializer and logs exceptions
                return (gclass) -> {
                    try {
                        method.invoke(null, gclass);
                    } catch (Exception e) {
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Exception in %s class init: %s\n", cls.getName(), e.toString());
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
                            "Exception in %s interface init: %s\n", cls.getName(), e.toString());
                }
            };
        }
        return null;
    }

    public static <T extends GObject, TC extends GObject.ObjectClass> Consumer<TC> overrideClassMethods(Class<T> cls) {
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

    private static <T extends GObject, TC extends GObject.ObjectClass> Consumer<TC> installProperties(Class<T> cls) {
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
                ps = GObjects.paramSpecGtype(p.name(), p.name(), p.name(), Type.G_TYPE_NONE, getFlags(p));
            } else if (p.type().equals(ParamSpecInt.class)) {
                ps = GObjects.paramSpecInt(p.name(), p.name(), p.name(), Integer.MIN_VALUE, Integer.MAX_VALUE, 0, getFlags(p));
            } else if (p.type().equals(ParamSpecInt64.class)) {
                ps = GObjects.paramSpecInt64(p.name(), p.name(), p.name(), Long.MIN_VALUE, Long.MAX_VALUE, 0, getFlags(p));
            } else if (p.type().equals(ParamSpecLong.class)) {
                ps = GObjects.paramSpecLong(p.name(), p.name(), p.name(), Long.MIN_VALUE, Long.MAX_VALUE, 0, getFlags(p));
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
                ps = GObjects.paramSpecUlong(p.name(), p.name(), p.name(), 0, Long.MAX_VALUE, 0, getFlags(p));
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

    /**
     * Register a new GType for a Java class. The GType will inherit from the GType of the Java
     * superclass (using {@link Class#getSuperclass()}, and invoking {@code getType()} and
     * {@code getMemoryLayout()} using reflection).
     * <p>
     * The name of the new GType will be the simple name of the Java class, but can also be
     * specified with the {@link RegisteredType} annotation. (All invalid characters, including '.',
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
    public static <T extends GObject, TC extends GObject.ObjectClass> Type register(Class<T> cls) {
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
            Consumer<TC> propertiesInit = installProperties(cls);
            Consumer<TC> classInit = getClassInit(cls);
            MemoryLayout instanceLayout = getInstanceLayout(cls, typeName);
            Consumer<T> instanceInit = getInstanceInit(cls);
            Function<MemorySegment, T> constructor = getAddressConstructor(cls);
            TypeFlags flags = getTypeFlags(cls);

            if (parentType == null || classLayout == null || instanceLayout == null
                    || constructor == null || flags == null) {
                GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                        "Cannot register type %s\n", cls.getName());
                return null;
            }

            // Generate default init function
            if (instanceInit == null) instanceInit = $ -> {};

            // Override virtual methods and install properties before running
            // a user-defined class init. We chain the generated initializers
            // (if not null) and default to an empty method _ -> {}.
            Consumer<TC> init = chain(overridesInit, propertiesInit);
            init = chain(init, classInit);
            classInit = init != null ? init : $ -> {};

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
                    ifaceInit = chain(ifaceOverridesInit, ifaceInit);
                    if (ifaceInit == null) {
                        ifaceInit = $ -> {};
                    }

                    Consumer<TypeInterface> finalIfaceInit = ifaceInit;
                    interfaceInfo.writeInterfaceInit((ti, data) -> finalIfaceInit.accept(ti));
                    GObjects.typeAddInterfaceStatic(type, ifaceType, interfaceInfo);
                }
            }

            return type;

        } catch (Exception e) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot register type %s: %s\n", cls.getName(), e.toString());
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
    public static <T extends GObject, TC extends GObject.ObjectClass> Type register(
            org.gnome.glib.Type parentType,
            String typeName,
            MemoryLayout classLayout,
            Consumer<TC> classInit,
            MemoryLayout instanceLayout,
            Consumer<T> instanceInit,
            Function<MemorySegment, T> constructor,
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

    // Chain two methods (if not null)
    private static <Z> Consumer<Z> chain(Consumer<Z> orig, Consumer<Z> other) {
        if (orig != null && other != null) {
            return orig.andThen(other);
        }
        return orig != null ? orig : other;
    }
}
