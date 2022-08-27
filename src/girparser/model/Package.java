package girparser.model;

public class Package extends GirElement {

    public String name;

    public Package(GirElement parent, String name) {
        super(parent);
        this.name = name;

    }
}
