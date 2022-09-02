package io.github.jwharm.javagi.interop;

import jdk.incubator.foreign.MemoryAddress;

public class Proxy implements NativeAddress {

    private final MemoryAddress handle;

    public Proxy(MemoryAddress handle) {
        this.handle = handle;
    }

    @Override
    public MemoryAddress HANDLE() {
        return handle;
    }
}
