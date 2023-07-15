package io.github.jwharm.javagi.base;

import io.github.jwharm.javagi.interop.MemoryCleaner;

import java.lang.foreign.MemorySegment;

/**
 * Base type for a Java proxy object to an instance in native memory.
 */
public class ProxyInstance implements Proxy {

    private final MemorySegment address;
    private boolean callParent;

    /**
     * Create a new {@code ProxyInstance} object for an instance in native memory.
     * @param address the memory address of the instance
     */
    public ProxyInstance(MemorySegment address) {
        this.address = address;
        this.callParent = false;
        MemoryCleaner.register(this);
    }

    /**
     * Set the flag that determines if for virtual method calls, {@code g_type_class_peek_parent()}
     * is used to obtain the function pointer of the parent type instead of the instance class.
     * @param callParent true if you want to call the parent vfunc instead of an overrided vfunc
     */
    protected void callParent(boolean callParent) {
        this.callParent = callParent;
    }

    /**
     * Returns the flag that determines if for virtual method calls, {@code g_type_class_peek_parent()}
     * is used to obtain the function pointer of the parent type instead of the instance class.
     * @return true when parent vfunc is called instead of an overrided vfunc, or false when the
     *         overrided vfunc of the instance is called.
     */
    public boolean callParent() {
        return this.callParent;
    }

    /**
     * Get the memory address of the instance
     * @return the memory address of the instance
     */
    @Override
    public MemorySegment handle() {
        return address;
    }

    /**
     * Returns the hashcode of the memory address
     * @return the hashcode of the memory address
     * @see MemorySegment#hashCode()
     */
    @Override
    public int hashCode() {
        return address.hashCode();
    }

    /**
     * Checks whether the other object is a ProxyInstance instance and the memory 
     * addresses are equal.
     * @param obj another object
     * @return true when the other object is a ProxyInstance instance and the 
     *         memory addresses are equal, otherwise false.
     * @see MemorySegment#equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof ProxyInstance other
                && address.equals(other.address);
    }
}
