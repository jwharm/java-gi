package io.github.jwharm.javagi.model;

public class Function extends Method {

    public Function(GirElement parent, String name, String cIdentifier, String deprecated, String throws_, String movedTo) {
        super(parent, name, cIdentifier, deprecated, throws_, null, null, movedTo);
    }
}
