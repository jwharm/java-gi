package io.github.jwharm.javagi.interop;

import java.lang.foreign.Addressable;
import java.lang.foreign.MemoryAddress;
import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.github.jwharm.javagi.base.RefCleaner;
import io.github.jwharm.javagi.util.ListIndexModel;
import org.gnome.glib.GLib;
import org.gnome.gobject.GObject;

import io.github.jwharm.javagi.base.Proxy;
import org.gnome.gobject.InitiallyUnowned;
import org.jetbrains.annotations.Nullable;

/**
 * Caches Proxy instances so the same instance is used for the same memory address.
 */
public class InstanceCache {

    public final static Map<Addressable, Proxy> strongReferences = new HashMap<>();
    public final static Map<Addressable, WeakReference<Proxy>> weakReferences = new HashMap<>();

    private static final Cleaner CLEANER = Cleaner.create();

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
        Proxy instance = strongReferences.get(address);
        if (instance != null) {
            return instance;
        }
        WeakReference<Proxy> weakRef = weakReferences.get(address);
        if (weakRef != null && weakRef.get() != null) {
            return weakRef.get();
        }

        // Get constructor from the type registry
        Function<Addressable, ? extends Proxy> ctor = TypeCache.getConstructor((MemoryAddress) address, fallback);

        // No instance in cache: Create a new instance
        Proxy newInstance = ctor.apply(address);

        // Null check on the new instance
        if (newInstance == null) {
            return null;
        }

        return put(address, newInstance);
    }

    public static Proxy put(Addressable address, Proxy newInstance) {
        // Do not put a new instance if it already exists
        if (strongReferences.containsKey(address) || weakReferences.containsKey(address)) {
            return newInstance;
        }

        GLib.printerr("Register " + address + " / " + newInstance + "\n");

        // Put the instance in the cache. If another thread did this (while we were creating a new
        // instance), putIfAbsent() will return that instance.
        Proxy existingInstance = strongReferences.putIfAbsent(address, newInstance);
        if (existingInstance != null) {
            return existingInstance;
        }

        // If the Proxy is a GObject instance, replace the reference with a toggle reference
        if (newInstance instanceof GObject gobject) {

            // Sink floating references first
            if (newInstance instanceof InitiallyUnowned) {
                gobject.refSink();
            }

            ToggleNotify notify = new ToggleNotify();
            gobject.addToggleRef(notify);
            gobject.unref();

            // Register a cleaner that will remove the toggle reference
            CLEANER.register(newInstance, new RefCleaner(address, notify));
        }

        // Return the new instance.
        return newInstance;
    }

    /**
     * A ToggleNotify implementation that re-uses the created function pointer
     */
    private static class ToggleNotify implements org.gnome.gobject.ToggleNotify {

        private MemoryAddress address;

        @Override
        public void run(@Nullable MemoryAddress data, GObject object, boolean isLastRef) {
            var key = object.handle();
            if (isLastRef) {
                GLib.printerr("Toggle " + object.handle() + " to strong\n");
                strongReferences.remove(key);
                weakReferences.put(key, new WeakReference<>(object));
            } else {
                GLib.printerr("Toggle " + object.handle() + " to weak\n");
                weakReferences.remove(key);
                strongReferences.put(key, object);
            }
        }

        @Override
        public MemoryAddress toCallback() {
            if (address == null) {
                address = org.gnome.gobject.ToggleNotify.super.toCallback();
            }
            return address;
        }
    }
}
