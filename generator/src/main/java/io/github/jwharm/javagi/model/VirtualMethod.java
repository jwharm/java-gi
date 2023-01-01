package io.github.jwharm.javagi.model;

public class VirtualMethod extends Method {

    public VirtualMethod(GirElement parent, String name, String deprecated, String throws_) {
        super(parent, name, null, deprecated, throws_, null, null);
    }
}
