package io.github.jwharm.javagi.model;

// Not implemented
public class FunctionMacro extends GirElement implements CallableType {

    public final String name, cIdentifier, introspectable, deprecated, throws_;
    public ReturnValue returnValue;
    public Parameters parameters;

    public FunctionMacro(GirElement parent, String name, String cIdentifier, String introspectable,
                         String deprecated, String throws_) {
        super(parent);
        this.name = name;
        this.cIdentifier = cIdentifier;
        this.introspectable = introspectable;
        this.deprecated = deprecated;
        this.throws_ = throws_;
    }

    @Override
    public Parameters getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Parameters ps) {
        this.parameters = ps;
    }

    @Override
    public ReturnValue getReturnValue() {
        return returnValue;
    }

    @Override
    public void setReturnValue(ReturnValue rv) {
        this.returnValue = rv;
    }
}
