package io.github.jwharm.javagi.interop;

import jdk.incubator.foreign.MemoryAddress;

public interface NativeAddress {

    void setHANDLE(MemoryAddress handle);

    MemoryAddress HANDLE();

    default boolean equals(ResourceProxy nativeAddress) {
        return HANDLE() != null && HANDLE().equals(nativeAddress.HANDLE());
    }
}
