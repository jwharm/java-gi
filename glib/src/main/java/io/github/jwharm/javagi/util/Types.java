package io.github.jwharm.javagi.util;

import io.github.jwharm.javagi.annotations.ClassInitializer;
import io.github.jwharm.javagi.annotations.CustomType;
import io.github.jwharm.javagi.annotations.InstanceInitializer;
import io.github.jwharm.javagi.base.ObjectProxy;
import io.github.jwharm.javagi.interop.TypeCache;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.gnome.gobject.GObject;
import org.gnome.gobject.GObjects;
import org.gnome.gobject.TypeClass;
import org.gnome.gobject.TypeFlags;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryLayout;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;
import java.util.function.Function;

public class Types {

    private static final String LOG_DOMAIN = "java-gi";

    public static String getName(Class<?> cls) {
        // Default type name: simple Java class name
        String typeNameInput = cls.getSimpleName();

        // Check for an annotation that overrides the type name
        if (cls.isAnnotationPresent(CustomType.class)) {
            var annotation = cls.getAnnotation(CustomType.class);
            if (annotation.name() != null) {
                typeNameInput = annotation.name();
            }
        }

        // Create type name. Replace all characters except a-z or A-Z with underscores
        return typeNameInput.replaceAll("[^a-zA-Z]", "_");
    }

    public static <T extends GObject> MemoryLayout getInstanceLayout(Class<T> cls, String typeName) {
        try {
            // Get instance-memorylayout of this class
            MemoryLayout instanceLayout;
            Method getLayoutMethod;
            try {
                getLayoutMethod = cls.getDeclaredMethod("getMemoryLayout");
                return (MemoryLayout) getLayoutMethod.invoke(null);
            } catch (Exception e) {
                // If no memory layout was defined, create a default memory layout
                // that only has a pointer to the parent class' memory layout.
                getLayoutMethod = cls.getSuperclass().getDeclaredMethod("getMemoryLayout");
                MemoryLayout parentLayout = (MemoryLayout) getLayoutMethod.invoke(null);
                return MemoryLayout.structLayout(
                        parentLayout.withName("parent_instance")
                ).withName(typeName);
            }
        } catch (Exception e) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot get instance memory layout for type %s: %s",
                    cls == null ? "null" : cls.getName(), e.getMessage());
            return null;
        }
    }

    public static <T extends GObject> MemoryLayout getClassLayout(Class<T> cls, String typeName) {
        try {
            // Get the typeclass. This is an inner class that extends TypeClass.
            // If the typeclass is unavailable, get it from the parent class.
            Class<?> typeClass = null;
            for (Class<?> gclass : cls.getClasses()) {
                if (TypeClass.class.isAssignableFrom(gclass)) {
                    typeClass = gclass;
                    break;
                }
            }
            if (typeClass == null) {
                for (Class<?> gclass : cls.getSuperclass().getClasses()) {
                    if (TypeClass.class.isAssignableFrom(gclass)) {
                        typeClass = gclass;
                        break;
                    }
                }
            }

            // Get class-memorylayout
            Method getLayoutMethod = typeClass.getDeclaredMethod("getMemoryLayout");
            MemoryLayout parentLayout = (MemoryLayout) getLayoutMethod.invoke(null);
            return MemoryLayout.structLayout(
                    parentLayout.withName("parent_class")
            ).withName(typeName + "Class");

        } catch (Exception e) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot get class memory layout for type %s: %s",
                    cls == null ? "null" : cls.getName(), e.getMessage());
            return null;
        }
    }

    public static <T extends GObject> Type getParentGType(Class<T> cls) {
        try {
            // Get GType of the parent class
            Method getTypeMethod = cls.getSuperclass().getDeclaredMethod("getType");
            return (Type) getTypeMethod.invoke(null);

        } catch (Exception e) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot get parent GType for type %s: %s",
                    cls == null ? "null" : cls.getName(), e.getMessage());
            return null;
        }
    }

    public static <T extends GObject> Function<Addressable, T> getAddressConstructor(Class<T> cls) {
        try {
            // Get memory address constructor
            Constructor<T> ctor = cls.getConstructor(Addressable.class);

            // Create a wrapper function that will run the constructor and catch exceptions
            return (addr) -> {
                try {
                    return ctor.newInstance(addr);
                } catch (Exception e) {
                    return null;
                }
            };
        } catch (Exception e) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot get memory-address constructor for type %s: %s",
                    cls == null ? "null" : cls.getName(), e.getMessage());
            return null;
        }
    }

    public static <T extends GObject> Consumer<T> getInstanceInit(Class<T> cls) {
        // Find instance initializer function
        for (Method method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(InstanceInitializer.class)) {
                // Create a wrapper function that calls the instance initializer and logs exceptions
                return (inst) -> {
                    try {
                        method.invoke(null, inst);
                    } catch (Exception e) {
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Error in %s instance init: %s", cls.getName(), e.getMessage());
                    }
                };
            }
        }
        return null;
    }

    public static <T extends GObject> Consumer<TypeClass> getClassInit(Class<T> cls) {
        // Find class initializer function
        for (Method method : cls.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ClassInitializer.class)) {
                // Create a wrapper function that calls the class initializer and logs exceptions
                return (gclass) -> {
                    try {
                        method.invoke(null, gclass);
                    } catch (Exception e) {
                        GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                                "Error in %s class init: %s", cls.getName(), e.getMessage());
                    }
                };
            }
        }
        return null;
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
     * Use {@link ClassInitializer} and {@link InstanceInitializer} annotations on static methods
     * in the Java class to indicate that these are to be called during GObject class- and
     * instance initialization.
     * <p>
     * The {@link TypeFlags#ABSTRACT} and {@link TypeFlags#FINAL} flags are set for abstract and
     * final Java classes.
     * @return the new registered GType
     * @param <T> The class must be derived from GObject
     */
    public static <T extends GObject> Type register(Class<T> cls) {
        try {
            String typeName = getName(cls);
            MemoryLayout instanceLayout = getInstanceLayout(cls, typeName);
            Class<?> parentClass = cls.getSuperclass();
            Type parentType = getParentGType(cls);
            MemoryLayout classLayout = getClassLayout(cls, typeName);
            Function<Addressable, T> constructor = getAddressConstructor(cls);
            Consumer<T> instanceInit = getInstanceInit(cls);
            Consumer<TypeClass> classInit = getClassInit(cls);
            TypeFlags flags = getTypeFlags(cls);

            if (instanceInit == null) instanceInit = $ -> {};
            if (classInit == null) classInit = $ -> {};

            // Register and return the GType
            return register(
                    parentType,
                    typeName,
                    classLayout,
                    classInit,
                    instanceLayout,
                    instanceInit,
                    constructor,
                    flags
            );

        } catch (Exception e) {
            GLib.log(LOG_DOMAIN, LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot register type %s: %s",
                    cls == null ? "null" : cls.getName(), e.getMessage());
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
     * @param <T> the instance initializer function must accept the
     *            result of the memory address constructor
     */
    public static <T extends ObjectProxy> Type register(
            org.gnome.glib.Type parentType,
            String typeName,
            MemoryLayout classLayout,
            Consumer<TypeClass> classInit,
            MemoryLayout instanceLayout,
            Consumer<T> instanceInit,
            Function<Addressable, T> constructor,
            TypeFlags flags
    ) {
        Type type = GObjects.typeRegisterStaticSimple(
                parentType,
                typeName,
                (short) classLayout.byteSize(),
                // The data parameter is not used.
                (typeClass, data) -> classInit.accept(typeClass),
                (short) instanceLayout.byteSize(),
                // The instance parameter is a type-instance of T, so construct a T proxy instance.
                // The typeClass parameter is not used.
                (instance, typeClass) -> instanceInit.accept(constructor.apply(instance.handle())),
                flags
        );
        // Register the type and constructor in the cache
        TypeCache.register(type, constructor);
        return type;
    }
}