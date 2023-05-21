package io.github.jwharm.javagi.types;

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
import java.lang.reflect.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The Types class contains static methods to register a Java class as a new GObject type.
 */
public class Types {

    /**
     * The domain to use for GLib error logging
     */
    static final String LOG_DOMAIN = "java-gi";

    /**
     * Convert the name of a class to the name for a new GType. When the
     * {@link RegisteredType} annotation is present, and the name parameter of
     * the annotation is not empty, it will be returned. Otherwise, the
     * package and class name will be used as the new GType name (with all
     * characters except a-z and A-Z converted to underscores).
     * @param cls the class for which a GType name is returned
     * @return the GType name
     */
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

    /**
     * Return the memory layout defined in the provided class, or if not found,
     * a new {@code MemoryLayout.structLayout} with one field that points to the
     * memory layout defined in the direct superclass.
     * @param cls the class to provide a memory layout for
     * @param typeName the name given tot the generated memory layout
     * @return the declared memory layout, or if not found, a generated memory layout
     * that copies the memory layout declared in the direct superclass.
     * @param <T> the class must extend {@link org.gnome.gobject.GObject}
     */
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

    /**
     * Return the inner TypeClass class, or the inner TypeClass from the superclass, or null if not found.
     * @param cls the class that contains (or whose superclass contains) an inner TypeClass class
     * @return the TypeClass class, or null if not found
     * @param <T> the parameter must extend {@link org.gnome.gobject.TypeInstance}
     * @param <TC> the returned class extends {@link org.gnome.gobject.TypeClass}
     */
    @SuppressWarnings("unchecked")
    public static <T extends TypeInstance, TC extends TypeClass> Class<TC> getTypeClass(Class<T> cls) {
        // Get the type-struct. This is an inner class that extends ObjectClass.
        for (Class<?> gclass : cls.getDeclaredClasses()) {
            if (TypeClass.class.isAssignableFrom(gclass)) {
                return (Class<TC>) gclass;
            }
        }
        // If the type-struct is unavailable, get it from the parent class.
        for (Class<?> gclass : cls.getSuperclass().getDeclaredClasses()) {
            if (TypeClass.class.isAssignableFrom(gclass)) {
                return (Class<TC>) gclass;
            }
        }
        return null;
    }

    /**
     * Return the inner TypeInterface class, or null if not found.
     * @param iface the interface that contains an inner TypeInterface class
     * @return the TypeInterface class, or null if not found
     * @param <TI> the returned class extends {@link org.gnome.gobject.TypeInterface}
     */
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
     * @param cls the class to get a memory layout for
     * @param typeName the name of the new memory layout
     * @return the requested memory layout
     * @param <T> the class must extend {@link org.gnome.gobject.GObject}
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

    /**
     * Return the {@link org.gnome.glib.Type} that is returned by a field with @GType
     * annotation, or if that annotation is not found, by searching for a field with type
     * {@code org.gnome.glib.Type}, or else, return null.
     * @param cls the class for which to return the declared GType
     * @return the declared GType
     */
    public static Type getGType(Class<?> cls) {
        Field gtypeField = null;

        // Find a field that is annotated with @GType and read its value
        for (Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(GType.class)) {
                gtypeField = field;
            }
        }

        // Find a field with type org.gnome.glib.Type.class
        for (Field field : cls.getDeclaredFields()) {
            if (field.getType().equals(org.gnome.glib.Type.class)) {
                gtypeField = field;
            }
        }

        if (gtypeField == null) {
            // No gtype found
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot find GType field in class %s\n",
                    cls.getName());
            return null;
        }

        try {
            return (Type) gtypeField.get(null);
        } catch (IllegalAccessException e) {
            // Field is not public
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "IllegalAccessException while trying to read %s.%s\n",
                    cls.getName(), gtypeField.getName());
            return null;
        }
    }

    /**
     * Return the MemoryLayout that is returned by a method with @MemoryLayout annotation,
     * or if that annotation is not found, by invoking {@code cls.getMemoryLayout()} if
     * such method exists, or else, return null.
     * @param cls the class for which to return the declared MemoryLayout
     * @return the declared MemoryLayout
     */
    public static MemoryLayout getLayout(Class<?> cls) {
        // Find a method that is annotated with @MemoryLayout and execute it
        for (Method method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Layout.class)) {
                // Check method signature
                if ((method.getParameterTypes().length != 0)
                        || (! method.getReturnType().equals(MemoryLayout.class))) {
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Method %s.%s does not have expected signature () -> MemoryLayout\n",
                            cls.getName(), method.getName());
                    return null;
                }
                // Invoke the @MemoryLayout-annotated method and return the result
                try {
                    return (MemoryLayout) method.invoke(null);
                } catch (IllegalAccessException e) {
                    // Method is not public
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "IllegalAccessException when calling %s.%s\n",
                            cls.getName(), method.getName());
                    return null;
                } catch (InvocationTargetException e) {
                    // Method throws an exception
                    Throwable t = e.getTargetException();
                    GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                            "Exception when calling %s.%s: %s\n",
                            cls.getName(), method.getName(), t.toString());
                    return null;
                }
            }
        }

        // Find a method {@code public static MemoryLayout getMemoryLayout()} and execute it
        try {
            // invoke getMemoryLayout() on the class
            Method getLayoutMethod = cls.getDeclaredMethod("getMemoryLayout");
            return (MemoryLayout) getLayoutMethod.invoke(null);

        } catch (Exception notfound) {
            return null;
        }
    }

    /**
     * Return the memory address constructor for the provided class. This is a constructor
     * for a new Proxy instance for a native memory address
     * @param cls the class that declares a constructor with a single {@link MemorySegment} parameter
     * @return the memory address constructor for this class, or null if not found
     * @param <T> the class must implement the {@link Proxy} interface
     */
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

    /**
     * Return a lambda that invokes the instance initializer, with is a method
     * that is annotated with {@link InstanceInit} and takes a single parameter
     * of type {@link GObject}.
     * @param cls the class that declares the instance init method
     * @return the instance initializer, or null if not found
     * @param <T> the class must extend {@link org.gnome.gobject.GObject}
     */
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

    /**
     * Return a lambda that invokes the class initializer, with is a method
     * that is annotated with {@link ClassInit} and takes a single parameter
     * of type {@link GObject.ObjectClass}.
     * @param cls the class that declares the class init method
     * @return the class initializer, or null if not found
     * @param <T> the class must extend {@link org.gnome.gobject.GObject}
     * @param <TC> the class initializer must accept a {@link GObject.ObjectClass} parameter
     */
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

    /**
     * Return a lambda that invokes the interface initializer, with is a method
     * that is annotated with {@link InterfaceInit} and takes a single parameter
     * of the type that is specified with {@code iface}.
     * @param cls the class that declares the interface init method
     * @return the interface initializer, or null if not found
     * @param <T> the class must extend {@link org.gnome.gobject.GObject}
     * @param <TI> the iface parameter must extend {@link TypeInterface}
     */
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

    /**
     * Construct a {@link TypeFlags} bitfield that specifies
     * whether the provided class is abstract and/or final.
     * @param cls the class for which to generate typeflags
     * @return the generated typeflags
     */
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
     * superclass (using {@link Class#getSuperclass()}, reading a {@link GType} annotated field and
     * executing {@code getMemoryLayout()} using reflection).
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
            Consumer<TC> overridesInit = Overrides.overrideClassMethods(cls);
            Consumer<TC> propertiesInit = Properties.installProperties(cls);
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
                    Consumer<TypeInterface> ifaceOverridesInit = Overrides.overrideInterfaceMethods(cls, iface);
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

    /* When both lambdas are not null: Return first.andThen(second)
     * When only first is not null: Return first
     * When only second is not null: Return second
     * When both lambdas are null: Return null
     */
    public static <Z> Consumer<Z> chain(Consumer<Z> first, Consumer<Z> second) {
        if (first != null && second != null) {
            return first.andThen(second);
        }
        return first != null ? first : second;
    }
}
