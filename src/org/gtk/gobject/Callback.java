package org.gtk.gobject;

import jdk.incubator.foreign.MemoryAddress;
import org.gtk.interop.NativeAddress;
import org.gtk.interop.ResourceProxy;

public class Callback extends ResourceProxy {

    public Callback(MemoryAddress handle) {
        super(handle);
    }
}
