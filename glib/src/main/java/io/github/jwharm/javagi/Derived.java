package io.github.jwharm.javagi;

import java.lang.foreign.*;

/**
 * This interface should be implemented by all classes that derive from a
 * Java-GI generated GObject Proxy class.
 */
public interface Derived extends Proxy {

    // Generate the memory layout of Derived types
    private MemoryLayout getMemoryLayout() {
        try {
            Class<?> parent = getClass().getSuperclass();
            return MemoryLayout.structLayout(
                    ((MemoryLayout) parent.getMethod("getMemoryLayout").invoke(null)).withName("parent_instance"),
                    ValueLayout.JAVA_INT.withName("value_object")
            );
        } catch (Exception e) {
            throw new InteropException(e);
        }
    }

    /**
     * Put (a reference to) a Java object in the GObject instance struct.
     * @param value the Javaobject to refer to
     */
    default void setValueObject(Object value) {
        MemoryLayout layout = getMemoryLayout();

        // Register the object in the cache. This returns the reference (hashcode)
        int hashCode = Interop.registerValueObject(value);

        // Store the object reference (hashcode) in the native struct
        layout.varHandle(MemoryLayout.PathElement.groupElement("value_object"))
                .set(MemorySegment.ofAddress((MemoryAddress) handle(), layout.byteSize(), Interop.getScope()), hashCode);
    }

    /**
     * Get the Java object that is referenced in the GObject instance struct.
     * @return The Java object that is referenced in the GObject instance struct, or {@code null} if no value was set.
     */
    default Object getValueObject() {
        MemoryLayout layout = getMemoryLayout();

        // Get the object reference (hashcode)
        int value = (int) layout.varHandle(MemoryLayout.PathElement.groupElement("value_object"))
                .get(MemorySegment.ofAddress((MemoryAddress) handle(), layout.byteSize(), Interop.getScope()));

        // Get the object from the cache
        return Interop.objectRegistry.get(value);
    }

    /**
     * Clear the object reference in the GObject instance struct.
     */
    default void clearValueObject() {
        MemoryLayout layout = getMemoryLayout();

        // Get the object reference (hashcode)
        int value = (int) layout.varHandle(MemoryLayout.PathElement.groupElement("value_object"))
                .get(MemorySegment.ofAddress((MemoryAddress) handle(), layout.byteSize(), Interop.getScope()));

        // Remove the object from the cache
        Interop.objectRegistry.remove(value);

        // Clear the object reference (set to 0)
        layout.varHandle(MemoryLayout.PathElement.groupElement("value_object"))
                .set(MemorySegment.ofAddress((MemoryAddress) handle(), layout.byteSize(), Interop.getScope()), 0);
    }
}
