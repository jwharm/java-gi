package io.github.jwharm.javagi.interop;

import jdk.incubator.foreign.MemoryAddress;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class ProxyFactory {

    public static final Set<Proxy> cache = Collections.newSetFromMap(
            new WeakHashMap<Proxy, Boolean>()
    );

    private static Proxy getFromCache(MemoryAddress address) {
        for (Proxy p : cache) {
            if (p.HANDLE().equals(address)) return p;
        }
        return null;
    }

    public static Proxy getCachedProxy(MemoryAddress address) {
        for (Proxy p : cache) {
            if (p.HANDLE().equals(address)) {
                return p;
            }
        }
        // Fallback
        System.out.println("Generating proxy for unknown memory address: " + address);
        Proxy proxy = new Proxy(address, false);
        cache.add(proxy);
        return proxy;
    }

    public static Proxy getProxy(MemoryAddress address, boolean owned) {
        for (Proxy p : cache) {
            if (p.HANDLE().equals(address)) {
                p.setOwnership(owned);
                return p;
            }
        }
        Proxy proxy = new Proxy(address, owned);
        cache.add(proxy);
        return proxy;
    }
}
