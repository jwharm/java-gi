package org.gtk.interop;

import jdk.incubator.foreign.MemoryAddress;

public interface NativeAddress {

    public void setHANDLE(MemoryAddress handle);

    public MemoryAddress HANDLE();

    default boolean equals(ResourceProxy nativeAddress) {
        return HANDLE() != null && HANDLE().equals(nativeAddress.HANDLE());
    }
}
