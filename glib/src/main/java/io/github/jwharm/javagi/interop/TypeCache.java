package io.github.jwharm.javagi.interop;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.gnome.glib.Type;

import io.github.jwharm.javagi.base.Proxy;

/**
 * A register of GTypes with a Java constructor for each GType.
 * Using this register, the correct Java class is always instantiated, based on the GType of 
 * the native object instance.
 */
public class TypeCache {
    
    private final static Map<Type, Function<Addressable, ? extends Proxy>> typeRegister = new HashMap<>();

    /**
     * Get the constructor from the type registry for the native object instance at the given 
     * memory address. The applicable constructor is determined based on the GType of the native 
     * object (as it was registered using {@link #register(Type, Function)}).
     * @param address Address of Proxy object to obtain the type from
     * @return the constructor, or {@code null} if address is {@null} or a null-pointer
     */
    public static Function<Addressable, ? extends Proxy> getConstructor(MemoryAddress address) {
        if (address == null || address.equals(MemoryAddress.NULL)) return null;
        Type type = Interop.getType(address);
        return typeRegister.get(type);
    }

    /**
     * Register the provided marshal function for the provided type
     * @param type Type to use as key in the type register
     * @param marshal Marshal function for this type
     */
    public static void register(Type type, Function<Addressable, ? extends Proxy> marshal) {
        typeRegister.put(type, marshal);
    }
    


}
