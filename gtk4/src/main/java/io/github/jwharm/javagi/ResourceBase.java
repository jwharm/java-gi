package io.github.jwharm.javagi;

import java.lang.foreign.Addressable;

public class ResourceBase implements Proxy {

    private Refcounted ref;

    public ResourceBase(Addressable address, Ownership ownership) {
        this.ref = Refcounted.get(address, ownership);
    }
    
    public Addressable handle() {
        return this.ref.handle();
    }

    public Refcounted refcounted() {
        return this.ref;
    }
}
