package girparser.model;

// Not implemented
public class FunctionMacro extends Method {

    public final String introspectable;

    public FunctionMacro(GirElement parent, String name, String cIdentifier, String introspectable,
                         String deprecated, String throws_) {
        super(parent, name, cIdentifier, deprecated, throws_);
        this.introspectable = introspectable;
    }
}
