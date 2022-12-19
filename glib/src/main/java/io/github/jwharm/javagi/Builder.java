package io.github.jwharm.javagi;

import java.util.ArrayList;

/**
 * Base class for all inner {@code Builder} classes inside GObject proxy classes.
 */
public class Builder {

    /**
     * List of all property names that are set
     */
    protected ArrayList<String> names = new ArrayList<>();
    
    /**
     * List of all property values that are set
     */
    protected ArrayList<org.gtk.gobject.Value> values = new ArrayList<>();
    
}
