package io.github.jwharm.javagi;

import java.lang.foreign.Addressable;

public interface Proxy {

    Addressable handle();
    Refcounted refcounted();

    default boolean equals(Proxy nativeAddress) {
        return handle() != null && handle().equals(nativeAddress.handle());
    }
}
