package org.gtk.gobject;

import jdk.incubator.foreign.MemoryAddress;

public class TypeModule extends Object {

    public static TypeModule castFrom(Object gobject) {
        return new TypeModule(gobject.HANDLE());
    }

    public TypeModule(MemoryAddress handle) {
        super(handle);
    }
}
