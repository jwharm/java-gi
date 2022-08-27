package org.gtk.gobject;

import jdk.incubator.foreign.MemoryAddress;

public class ParamSpec extends Object {

    public static ParamSpec castFrom(Object gobject) {
        return new ParamSpec(gobject.HANDLE());
    }

    public ParamSpec(MemoryAddress handle) {
        super(handle);
    }
}
