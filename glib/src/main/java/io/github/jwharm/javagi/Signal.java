package io.github.jwharm.javagi;

import org.gtk.gobject.GObject;

import java.lang.foreign.Addressable;

public class Signal<T> {

    private final org.gtk.gobject.Object instance;
    private final long handlerId;

    public Signal(Addressable instance, long handlerId) {
        this.instance = (org.gtk.gobject.Object) org.gtk.gobject.Object.fromAddress.marshal(instance, Ownership.UNKNOWN);
        this.handlerId = handlerId;
    }

    public void block() {
        GObject.signalHandlerBlock(instance, handlerId);
    }

    public void unblock() {
        GObject.signalHandlerUnblock(instance, handlerId);
    }

    public void disconnect() {
        GObject.signalHandlerDisconnect(instance, handlerId);
    }

    public boolean isConnected() {
        return GObject.signalHandlerIsConnected(instance, handlerId);
    }
}
