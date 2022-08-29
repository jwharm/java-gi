package girparser.model;

public class Field extends GirElement {

    public final String readable, isPrivate;
    public Callback callback;

    public Field(GirElement parent, String name, String readable, String isPrivate) {
        super(parent);
        this.name = name;
        this.readable = readable;
        this.isPrivate = isPrivate;
    }
}
