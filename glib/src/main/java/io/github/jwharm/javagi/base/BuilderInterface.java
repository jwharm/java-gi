package io.github.jwharm.javagi.base;

import org.gtk.gobject.Value;

/**
 * Base interface for nested Builder types in interfaces.
 */
public interface BuilderInterface {

    /**
     * Add the provided property name and value to the builder
     * @param name name of the property
     * @param value value of the property (a {@code GValue})
     */
    void addBuilderProperty(String name, Value value);
}
