package org.gtk.gobject;

import jdk.incubator.foreign.MemoryAddress;

public class InitiallyUnowned extends Object {

    public static InitiallyUnowned castFrom(Object gobject) {
        return new InitiallyUnowned(gobject.HANDLE());
    }

    public InitiallyUnowned(MemoryAddress handle) {
        super(handle);
    }
}
