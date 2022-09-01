package io.github.jwharm.javagi.model;

public class Attribute extends GirElement {
    public final String type;

    public Attribute(GirElement parent, String name, String type) {
        super(parent);
        this.name = name;
        this.type = type;
    }
}
