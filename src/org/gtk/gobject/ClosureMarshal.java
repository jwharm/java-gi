package org.gtk.gobject;

import jdk.incubator.foreign.MemoryAddress;

public class ClosureMarshal extends Object {

    public static ClosureMarshal castFrom(Object gobject) {
        return new ClosureMarshal(gobject.HANDLE());
    }

    public ClosureMarshal(MemoryAddress handle) {
        super(handle);
    }
}
