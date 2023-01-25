package io.github.jwharm.javagi.base;

import org.gtk.gobject.Value;

public interface PropertyBuilder {

    void addBuilderProperty(String name, Value value);
}
