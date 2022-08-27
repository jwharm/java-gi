package girparser.model;

public class Constant extends GirElement {

    public String value, cType;

    public Constant(GirElement parent, String name, String value, String cType) {
        super(parent);
        this.name = name;
        this.value = value;
        this.cType = cType;
    }
}
