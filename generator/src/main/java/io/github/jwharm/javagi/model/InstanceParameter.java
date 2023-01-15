package io.github.jwharm.javagi.model;

public class InstanceParameter extends Parameter {

    public InstanceParameter(GirElement parent, String name, String transferOwnership, String nullable, String allowNone) {
        super(parent, name, transferOwnership, nullable, allowNone, null, null, null);
    }
}
