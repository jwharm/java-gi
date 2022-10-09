package io.github.jwharm.javagi;

import java.lang.foreign.Addressable;

public class ResourceBase implements Proxy {

    private Refcounted ref;

    public ResourceBase(Refcounted ref) {
        this.ref = ref;
    }

    public Addressable handle() {
        return this.ref.handle();
    }

    public Refcounted refcounted() {
        return this.ref;
    }
}
