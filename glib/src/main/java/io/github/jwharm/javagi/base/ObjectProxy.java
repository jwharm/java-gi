package io.github.jwharm.javagi.base;

import io.github.jwharm.javagi.interop.TypeCache;
import org.gnome.glib.GLib;
import org.gnome.glib.LogLevelFlags;
import org.gnome.glib.Type;
import org.gnome.gobject.GObject;
import org.gnome.gobject.GObjects;
import org.gnome.gobject.TypeClass;
import org.gnome.gobject.TypeFlags;
import org.gnome.gobject.TypeInstance;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryLayout;
import java.lang.ref.Cleaner;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Abstract base class for proxy objects that represent an object instance
 * in native memory. For ref-counted objects, a Cleaner is attached that
 * will unref the instance when the proxy instance has ownership and is
 * garbage-collected.
 */
public abstract class ObjectProxy extends TypeInstance {

    private static final String LOG_DOMAIN = "java-gi";
    private static final Cleaner CLEANER = Cleaner.create();
    private RefCleaner refCleaner;

    /**
     * Instantiate the ObjectProxy class.
     * @param address the memory address of the object in native memory
     */
    protected ObjectProxy(Addressable address) {
        super(address);
    }

    /**
     * Disable the Cleaner that automatically calls {@code g_object_unref} (or
     * another method that has been specified) when this object is garbage collected.
     */
    public void yieldOwnership() {
        if (refCleaner != null && refCleaner.registered) {
            refCleaner.registered = false;
        }
    }

    /**
     * Enable the Cleaner that automatically calls {@code g_object_unref} (or
     * another method that has been specified) when this object is garbage collected.
     */
    public void takeOwnership() {
        if (this.refCleaner == null) {
            refCleaner = new RefCleaner(handle());
            CLEANER.register(this, refCleaner);
        } else {
            refCleaner.registered = true;
        }
    }

    /**
     * Change the method name from the default {@code g_object_unref} to
     * another method name.
     * @param method the unref method name
     */
    public void setRefCleanerMethod(String method) {
        if (refCleaner != null) {
            refCleaner.refCleanerMethod = method;
            refCleaner.unrefMethodHandle = null;
        }
    }

    /**
     * Register a new GType for a Java class. The type will inherit from the type of the Java
     * superclass (using {@link Class#getSuperclass()} and invoking {@code getType()} and
     * {@code getMemoryLayout()} using reflection).
     * <p>
     * The name of the new GType will be the fully qualified name of the Java class (all invalid
     * characters like '.' are replaced with underscores).
     * <p>
     * The new GType does not have a custom memory layout, nor does it have a custom class
     * initializer or instance initializer method or any typeflags.
     * @return the new registered GType
     */
    public static Type registerType(Class<? extends GObject> cls) {
        try {
            // Create type name. Replace all characters except a-z or A-Z with underscores.
            String typeName = cls.getName().replaceAll("[^a-zA-Z]", "_");

            // Get parent class
            Class<?> parentClass = cls.getSuperclass();

            // Get instance-memorylayout of this class, or if not found, of the parent class
            Method getLayoutMethod;
            try {
                getLayoutMethod = cls.getDeclaredMethod("getMemoryLayout");
            } catch (NoSuchMethodException nme) {
                getLayoutMethod = parentClass.getDeclaredMethod("getMemoryLayout");
            }
            MemoryLayout instanceLayout = (MemoryLayout) getLayoutMethod.invoke(null);

            // Get GType of the parent class
            Method getTypeMethod = parentClass.getDeclaredMethod("getType");
            Type parentType = (Type) getTypeMethod.invoke(null);

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
                for (Class<?> gclass : parentClass.getClasses()) {
                    if (TypeClass.class.isAssignableFrom(gclass)) {
                        typeClass = gclass;
                        break;
                    }
                }
            }

            // Get class-memorylayout
            getLayoutMethod = typeClass.getDeclaredMethod("getMemoryLayout");
            MemoryLayout classLayout = (MemoryLayout) getLayoutMethod.invoke(null);

            // Get memory address constructor
            Constructor<? extends ObjectProxy> ctor = cls.getConstructor(Addressable.class);

            // Create a wrapper function that will run the constructor and catch exceptions
            Function<Addressable, ? extends ObjectProxy> func = (addr) -> {
                try {
                    return ctor.newInstance(addr);
                } catch (Exception e) {
                    return null;
                }
            };

            // Register and return the GType
            return registerType(
                    parentType,
                    typeName,
                    classLayout,
                    $ -> {},
                    instanceLayout,
                    $ -> {},
                    func,
                    TypeFlags.NONE
            );

        } catch (Exception e) {
            // Log exception and return null
            GLib.log(
                    LOG_DOMAIN,
                    LogLevelFlags.LEVEL_CRITICAL,
                    "Cannot register type: %s",
                    e.getMessage()
            );
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
    public static <T extends ObjectProxy> Type registerType(
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
