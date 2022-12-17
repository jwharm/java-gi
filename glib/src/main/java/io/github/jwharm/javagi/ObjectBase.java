package io.github.jwharm.javagi;

import org.gtk.glib.Type;
import org.gtk.gobject.TypeFlags;
import org.gtk.gobject.TypeInfo;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.Cleaner;

/**
 * Abastract base class for proxy objects that represent a GObject instance 
 * in native memory. The {@link #handle()} method returns the memory address
 * of the object.
 * For ref-counted objects where ownership is transferred to the user, a 
 * Cleaner is registered by the constructor that will automatically call 
 * {@code g_object_unref} when the proxy object is garbage-collected.
 */
public abstract class ObjectBase implements Proxy {

    private final Addressable address;
    private final Ownership ownership;
    private static final Cleaner cleaner = Cleaner.create();
    private State state;
    private Cleaner.Cleanable cleanable;

    // Method handle that is used for the g_object_unref native call
    private static final MethodHandle g_object_unref = Interop.downcallHandle(
            "g_object_unref",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
            false
    );

    // The State class is used by the Cleaner
    private static class State implements Runnable {
        Addressable address;
        boolean registered;

        State(Addressable address) {
            this.address = address;
            this.registered = true;
        }

        public void run() {
            if (registered) {
                try {
                    // Debug logging
                    // System.out.println("g_object_unref " + address);
                    
                    g_object_unref.invokeExact(address);
                    
                } catch (Throwable err) {
                    throw new AssertionError("Unexpected exception occured: ", err);
                }
            }
        }
    }
    
    /**
     * Instantiate the Proxy base class. When ownership is FULL, a cleaner is registered
     * to automatically call g_object_unref on the memory address.
     * @param address   The memory address of the object in native memory
     * @param ownership The ownership status. When ownership is FULL, a cleaner is registered
     *                  to automatically call g_object_unref on the memory address.
     */
    protected ObjectBase(Addressable address, Ownership ownership) {
        this.address = address;
        this.ownership = ownership;
        if (ownership == Ownership.FULL) {
            state = new State(address);
            cleanable = cleaner.register(this, state);
        }
        
        // Debug logging
        // System.out.printf("New: %s %s %s\n", address, this.getClass().getName(), ownership);
    }

    /**
     * Return the memory address of the object in native memory
     * @return The native memory address of the object
     */
    public Addressable handle() {
        return this.address;
    }
    
    /**
     * Disable the Cleaner that automatically calls {@code g_object_unref} 
     * when this object is garbage collected, and return the ownership 
     * indicator.
     * @return The ownership indicator of this object
     */
    public Ownership yieldOwnership() {
        
        // Debug logging
        // System.out.printf("Yield ownership for address %s\n", address);
        
        if (this.state != null && this.state.registered) {
            this.state.registered = false;
        }
        return this.ownership;
    }

    /**
     * This function is called during class initialization of GObject-derived classes.
     * It is a no-op, but you can define the same function in a derived class to
     * add an implementation for that class.
     * @param gClass Pointer to the class struct
     * @param classData Always {@link MemoryAddress#NULL}
     */
    protected static void classInit(MemoryAddress gClass, MemoryAddress classData) {
    }

    /**
     * This function is called during instance initialization of GObject-derived class
     * instances. It is a no-op, but you can define the same function in a derived class
     * to add an implementation for that class instance.
     * @param instance Pointer to the instance struct
     * @param gClass Pointer to the class struct
     */
    protected static void init(MemoryAddress instance, MemoryAddress gClass) {
    }

    /**
     * Registers the provided class as a GType.
     * See {@link org.gtk.gobject.GObject#typeRegisterStatic(Type, String, TypeInfo, TypeFlags)}
     * @param c The class to register. The class must implement the {@link Derived} interface.
     * @return the registered {@link org.gtk.glib.Type}
     */
    public static Type register(Class<? extends Derived> c) {
        try {
            Class<?> parentClass = c.getSuperclass();
            Type parentGType = (Type) parentClass.getMethod("getType").invoke(null);
            Class<?> parentTypeClass = Class.forName(parentClass.getName() + "Class");

            // Create memorylayout for typeclass struct
            MemoryLayout classMemoryLayout = MemoryLayout.structLayout(
                    ((MemoryLayout) parentTypeClass.getMethod("getMemoryLayout").invoke(null)).withName("parent_class")
            ).withName(c.getSimpleName() + "Class");
            short classSize = Long.valueOf(classMemoryLayout.byteSize()).shortValue();

            // Create memorylayout for typeinstance struct
            MemoryLayout instanceMemoryLayout = MemoryLayout.structLayout(
                    ((MemoryLayout) parentClass.getMethod("getMemoryLayout").invoke(null)).withName("parent_instance"),
                    ValueLayout.JAVA_INT.withName("value_object")
            ).withName(c.getSimpleName());
            short instanceSize = Long.valueOf(instanceMemoryLayout.byteSize()).shortValue();

            // Create upcall stub for class init
            MemoryAddress classInit = Linker.nativeLinker().upcallStub(
                    MethodHandles.lookup().findStatic(c, "classInit",
                            MethodType.methodType(void.class, MemoryAddress.class, MemoryAddress.class)),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    Interop.getScope()
            ).address();

            // Create upcall stub for instance init
            MemoryAddress instanceInit = Linker.nativeLinker().upcallStub(
                    MethodHandles.lookup().findStatic(c, "init",
                            MethodType.methodType(void.class, MemoryAddress.class, MemoryAddress.class)),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    Interop.getScope()
            ).address();

            // Create TypeInfo struct
            TypeInfo typeInfo = TypeInfo.builder()
                    .setBaseInit(null)
                    .setBaseFinalize(null)
                    .setClassSize(classSize)
                    .setClassInit(classInit)
                    .setClassFinalize(null)
                    .setClassData(null)
                    .setInstanceSize(instanceSize)
                    .setInstanceInit(instanceInit)
                    .setNPreallocs((short) 0)
                    .setValueTable(null)
                    .build();

            // Call GObject.typeRegisterStatic and return the generated GType
            return org.gtk.gobject.GObject.typeRegisterStatic(parentGType, c.getSimpleName(), typeInfo, new TypeFlags(0));

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
