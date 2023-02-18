package io.github.jwharm.javagi.interop;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.gnome.gobject.GObject;

import io.github.jwharm.javagi.base.Proxy;

/**
 * Caches Proxy instances so the same instance is used for the same memory address.
 */
public class InstanceCache {
    
    private final static Map<Addressable, Proxy> instanceCache = new HashMap<>();

    /**
     * Get a {@link Proxy} object for the provided native memory address. If a Proxy object does  
     * not yet exist for this address, a new Proxy object is instantiated and added to the cache. 
     * Invalid references are removed from the cache using a GObject toggle reference.
     * @param address  memory address of the native object
     * @param fallback fallback constructor to use when the type is not found in the TypeCache
     * @return         a Proxy instance for the provided memory address
     */
    public static Proxy get(Addressable address, Function<Addressable, ? extends Proxy> fallback) {
        // Null check on the memory address
        if (address == null || address.equals(MemoryAddress.NULL)) {
            return null;
        }
        
        // Get instance from cache
        Proxy instance = instanceCache.get(address);
        if (instance != null) {
            return instance;
        }
        
        // Get constructor from the type registry
        Function<Addressable, ? extends Proxy> ctor = TypeCache.getConstructor((MemoryAddress) address, fallback);

        // No instance in cache: Create a new instance
        Proxy newInstance = ctor.apply(address);
        
        // Null check on the new instance
        if (newInstance == null) {
            return null;
        }
        
        // If the Proxy is a GObject instance, register a toggle reference
        if (newInstance instanceof GObject gobject) {
            gobject.addToggleRef((data, object, isLastRef) -> {
                if (isLastRef) {
                    instanceCache.remove(object.handle());
                }
            });
        }
        
        // Put the instance in the cache. If another thread did this (while we were creating a new 
        // instance), putIfAbsent() will return that instance.
        Proxy instanceFromAnotherThread = instanceCache.putIfAbsent(address, newInstance);
        
        // Return the instance that was already in the cache, or else the new instance.
        return Objects.requireNonNullElse(instanceFromAnotherThread, newInstance);
    }
}
