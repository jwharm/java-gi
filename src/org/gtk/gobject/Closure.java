package org.gtk.gobject;

import jdk.incubator.foreign.MemoryAddress;
import org.gtk.interop.ResourceProxy;

public class Closure extends ResourceProxy {

    public Closure(MemoryAddress handle) {
        super(handle);
    }
}
