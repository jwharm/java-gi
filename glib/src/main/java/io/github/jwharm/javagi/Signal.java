package io.github.jwharm.javagi;

import org.gtk.gobject.GObjects;

import java.lang.foreign.Addressable;

public class Signal<T> {

    private final org.gtk.gobject.GObject instance;
    private final long handlerId;

    public Signal(Addressable instance, long handlerId) {
        this.instance = new org.gtk.gobject.GObject(instance, Ownership.UNKNOWN);
        this.handlerId = handlerId;
    }

    public void block() {
        GObjects.signalHandlerBlock(instance, handlerId);
    }

    public void unblock() {
        GObjects.signalHandlerUnblock(instance, handlerId);
    }

    public void disconnect() {
        GObjects.signalHandlerDisconnect(instance, handlerId);
    }

    public boolean isConnected() {
        return GObjects.signalHandlerIsConnected(instance, handlerId);
    }
}
