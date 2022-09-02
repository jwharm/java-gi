package io.github.jwharm.javagi.interop;

import jdk.incubator.foreign.MemoryAddress;

public class ProxyFactory {

    public static Proxy getProxy(MemoryAddress address) {
        return new Proxy(address);
    }

}
