package io.github.jwharm.javagi;

import org.gtk.gobject.Value;

import java.util.ArrayList;

/**
 * Base class for all inner {@code Builder} classes inside GObject proxy classes.
 */
public class Builder implements PropertyBuilder {

    /**
     * List of all property names that are set
     */
    private ArrayList<String> names = new ArrayList<>();
    
    /**
     * List of all property values that are set
     */
    private ArrayList<org.gtk.gobject.Value> values = new ArrayList<>();

    public void addBuilderProperty(String name, Value value) {
        names.add(name);
        values.add(value);
    }

    public int getSize() {
        return names.size();
    }

    public String[] getNames() {
        return names.toArray(new String[getSize()]);
    }

    public Value[] getValues() {
        return values.toArray(new Value[getSize()]);
    }

}
