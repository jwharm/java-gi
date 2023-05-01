package io.github.jwharm.javagi.model;

public class Repository extends GirElement {

    public final Module module;
    public Namespace namespace = null;
    public Package package_ = null;

    public Repository(Module module) {
        super(null);
        this.module = module;
    }

    public boolean isApi() {
        return namespace.isApi();
    }
}
