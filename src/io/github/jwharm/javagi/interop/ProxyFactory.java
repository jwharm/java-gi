package io.github.jwharm.javagi.interop;

import jdk.incubator.foreign.MemoryAddress;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class ProxyFactory {

    private static final Set<Proxy> cache = Collections.newSetFromMap(
            new WeakHashMap<Proxy, Boolean>()
    );

    private static Proxy getFromCache(MemoryAddress address) {
        for (Proxy p : cache) {
            if (p.HANDLE().equals(address)) return p;
        }
        return null;
    }

    public static Proxy getProxy(MemoryAddress address) {
        for (Proxy p : cache) {
            if (p.HANDLE().equals(address)) {
                return p;
            }
        }
        Proxy proxy = new Proxy(address);
        cache.add(proxy);
        return proxy;
    }
}
