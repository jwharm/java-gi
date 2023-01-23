package io.github.jwharm.javagi;

import org.gtk.gobject.Value;

import java.util.ArrayList;

/**
 * Base class for all inner {@code Builder} classes inside GObject proxy classes.
 * @param <S> the type of the Builder that is returned
 */
public abstract class Builder<S extends Builder> implements PropertyBuilder {

    /**
     * List of all property names that are set
     */
    private final ArrayList<String> names = new ArrayList<>();
    
    /**
     * List of all property values that are set
     */
    private final ArrayList<Value> values = new ArrayList<>();

    /**
     * Add the provided property name and value to the builder
     * @param name name of the property
     * @param value value of the property (a {@code GValue})
     */
    public void addBuilderProperty(String name, Value value) {
        names.add(name);
        values.add(value);
    }

    /**
     * Get the property names
     * @return a {@code String} array of property names
     */
    public String[] getNames() {
        return names.toArray(new String[names.size()]);
    }

    /**
     * Get the property values
     * @return a {@code GValue} array of property names
     */
    public Value[] getValues() {
        return values.toArray(new Value[values.size()]);
    }

}
