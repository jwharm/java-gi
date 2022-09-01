package io.github.jwharm.javagi.model;

public class Property extends GirElement {

    public final String transferOwnership, getter;

    public Property(GirElement parent, String name, String transferOwnership, String getter) {
        super(parent);
        this.name = name;
        this.transferOwnership = transferOwnership;
        this.getter = getter;
    }
}
