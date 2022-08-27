package girparser.model;

public class Interface extends Class {

    public Prerequisite prerequisite;

    public Interface(GirElement parent, String name) {
        super(parent, name, null);
    }
}
