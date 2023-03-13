package io.github.jwharm.javagi.base;

import org.gnome.gobject.GObject;
import org.gnome.gobject.GObjects;

import io.github.jwharm.javagi.interop.InstanceCache;

import java.lang.foreign.Addressable;

/**
 * Represents a signal connection. With a {@code Signal} object, a signal connection
 * can be blocked, unblocked, and disconnected. It is also possible to check if the
 * signal is still connected.
 * @param <T> the type of the signal
 */
public class Signal<T> {

    private final org.gnome.gobject.GObject instance;
    private final int handlerId;

    /**
     * Create a Signal instance for the provided GObject instance and handler ID
     * @param instance the native memory address of the GObject instance
     * @param handlerId the handler ID of the signal
     */
    public Signal(Addressable instance, long handlerId) {
        this.instance = (GObject) InstanceCache.getForType(instance, GObject::new);
        this.handlerId = (int) handlerId;
    }

    /**
     * Blocks a handler of an instance so it will not be called during any
     * signal emissions unless it is unblocked again. Thus "blocking" a
     * signal handler means to temporarily deactivate it, a signal handler
     * has to be unblocked exactly the same amount of times it has been
     * blocked before to become active again.
     */
    public void block() {
        GObjects.signalHandlerBlock(instance, handlerId);
    }

    /**
     * Undoes the effect of a previous g_signal_handler_block() call.  A
     * blocked handler is skipped during signal emissions and will not be
     * invoked, unblocking it (for exactly the amount of times it has been
     * blocked before) reverts its "blocked" state, so the handler will be
     * recognized by the signal system and is called upon future or
     * currently ongoing signal emissions (since the order in which
     * handlers are called during signal emissions is deterministic,
     * whether the unblocked handler in question is called as part of a
     * currently ongoing emission depends on how far that emission has
     * proceeded yet).
     */
    public void unblock() {
        GObjects.signalHandlerUnblock(instance, handlerId);
    }

    /**
     * Disconnects a handler from an instance so it will not be called during
     * any future or currently ongoing emissions of the signal it has been
     * connected to. The {@code handler_id} becomes invalid and may be reused.
     */
    public void disconnect() {
        GObjects.signalHandlerDisconnect(instance, handlerId);
    }

    /**
     * Returns whether this signal is connected.
     * @return whether the {@code handler_id} of this signal identifies a handler connected to the {@code instance}.
     */
    public boolean isConnected() {
        return GObjects.signalHandlerIsConnected(instance, handlerId);
    }
}
