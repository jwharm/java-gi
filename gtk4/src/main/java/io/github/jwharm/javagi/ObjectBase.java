package io.github.jwharm.javagi;

import org.gtk.glib.Type;
import org.gtk.gobject.TypeFlags;
import org.gtk.gobject.TypeInfo;
import org.jetbrains.annotations.ApiStatus;

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
    private final static Cleaner cleaner = Cleaner.create();
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
                    
                } catch (Throwable ERR) {
                    throw new AssertionError("Unexpected exception occured: ", ERR);
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
    @ApiStatus.Internal
    public ObjectBase(Addressable address, Ownership ownership) {
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
    @ApiStatus.Internal
    public Addressable handle() {
        return this.address;
    }
    
    /**
     * Disable the Cleaner that automatically calls {@code g_object_unref} 
     * when this object is garbage collected, and return the ownership 
     * indicator.
     * @return The ownership indicator of this object
     */
    @ApiStatus.Internal
    public Ownership yieldOwnership() {
        
        // Debug logging
        // System.out.printf("Yield ownership for address %s\n", address);
        
        if (this.state != null && this.state.registered) {
            this.state.registered = false;
        }
        return this.ownership;
    }

    public static void initClass(MemoryAddress gClass, MemoryAddress classData) {
    }

    public static void initInstance(MemoryAddress instance, MemoryAddress gClass) {
    }
    
    public static Type register(Class<?> c) {
        try {
            Class<?> parentClass = c.getSuperclass();
            
            // Create memorylayout for typeinstance struct
            MemoryLayout instanceMemoryLayout = MemoryLayout.structLayout(
                    ((MemoryLayout) parentClass.getMethod("getMemoryLayout").invoke(null)).withName("parent_instance")
            ).withName(c.getSimpleName());
            short instanceSize = Long.valueOf(instanceMemoryLayout.byteSize()).shortValue();
            
            // Get parent typeinstance gtype
            org.gtk.glib.Type parentGType = (org.gtk.glib.Type) parentClass.getMethod("getType").invoke(null);

            // Get parent typeclass
            Class<?> parentTypeClass = null;
            if (parentClass.getName().equals("org.gtk.gobject.Object")) {
                parentTypeClass = org.gtk.gobject.TypeClass.class;
            } else {
                parentTypeClass = Class.forName(parentClass.getName() + "Class");
            }

            // Create memorylayout for typeclass struct
            MemoryLayout classMemoryLayout = MemoryLayout.structLayout(
                    ((MemoryLayout) parentTypeClass.getMethod("getMemoryLayout").invoke(null)).withName("parent_class")
            ).withName(c.getSimpleName() + "Class");
            short classSize = Long.valueOf(classMemoryLayout.byteSize()).shortValue();

            // Create upcall stub for class init
            MemoryAddress classInit = Linker.nativeLinker().upcallStub(
                    MethodHandles.lookup().findStatic(c, "initClass",
                            MethodType.methodType(void.class, MemoryAddress.class, MemoryAddress.class)),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    Interop.getScope()
            ).address();

            // Create upcall stub for instance init (constructor)
            MemoryAddress instanceInit = Linker.nativeLinker().upcallStub(
                    MethodHandles.lookup().findStatic(c, "initInstance",
                            MethodType.methodType(void.class, MemoryAddress.class, MemoryAddress.class)),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    Interop.getScope()
            ).address();

            // Create TypeInfo struct
            TypeInfo typeInfo = new TypeInfo.Build()
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
                    .construct();

            // Call GObject.typeRegisterStatic and return the generated GType
            return org.gtk.gobject.GObject.typeRegisterStatic(parentGType, c.getSimpleName(), typeInfo, TypeFlags.NONE);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
