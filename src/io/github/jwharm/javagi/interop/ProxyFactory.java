package io.github.jwharm.javagi.interop;

import jdk.incubator.foreign.MemoryAddress;

import java.util.WeakHashMap;

public class ProxyFactory {

    private static final WeakHashMap<MemoryAddress, Proxy> cache = new WeakHashMap<>();

    public static Proxy getProxy(MemoryAddress address) {
        Proxy proxy = cache.get(address);
        if (proxy == null) {
            System.out.println("New proxy for " + address);
            proxy = new Proxy(address);
            cache.put(address, proxy);
        } else {
            System.out.println("Re-use proxy for " + address);
        }
        return proxy;
    }
}
