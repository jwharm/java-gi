package org.gtk.gobject;

import jdk.incubator.foreign.MemoryAddress;

public class ClosureNotify extends Object {

    public static ClosureNotify castFrom(Object gobject) {
        return new ClosureNotify(gobject.HANDLE());
    }

    public ClosureNotify(MemoryAddress handle) {
        super(handle);
    }
}
