package io.github.jwharm.javagi.model;

public class Repository extends GirElement {

    public Namespace namespace = null;
    public Package package_ = null;

    public Repository() {
        super(null);
    }

    public boolean isApi() {
        return namespace.isApi();
    }
}
