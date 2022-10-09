package io.github.jwharm.javagi.model;

public class Array extends GirElement {

    public final String cType, zeroTerminated, fixedSize;

    public Array(GirElement parent, String name, String cType, String zeroTerminated, String fixedSize) {
        super(parent);
        this.name = name;
        this.cType = cType;
        this.zeroTerminated = zeroTerminated;
        this.fixedSize = fixedSize;
    }
}
