package io.github.jwharm.javagi.interop;

import jdk.incubator.foreign.MemoryAddress;

public class ResourceProxy implements NativeAddress {

    private MemoryAddress __HANDLE__;

    public ResourceProxy(MemoryAddress handle) {
        this.__HANDLE__ = handle;
    }

    public void setHANDLE(MemoryAddress handle) {
        this.__HANDLE__ = handle;
    }

    public MemoryAddress HANDLE() {
        return __HANDLE__;
    }
}
