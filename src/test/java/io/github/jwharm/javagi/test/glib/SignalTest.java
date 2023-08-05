package io.github.jwharm.javagi.test.glib;

import io.github.jwharm.javagi.base.SignalConnection;
import org.gnome.gio.Application;
import org.gnome.gio.ApplicationFlags;
import org.gnome.gobject.GObject;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test connecting a signal, and blocking/unblocking it
 */
public class SignalTest {

    @Test
    public void connectSignal() {
        var success = new AtomicBoolean(false);
        Application app = new Application("test.id1", ApplicationFlags.DEFAULT_FLAGS);
        SignalConnection<GObject.Notify> signal = app.onNotify("application-id", paramSpec -> {
            success.set(true);
        });
        assertTrue(signal.isConnected());
        app.setApplicationId("test.id2");
        assertTrue(success.get());
    }

    @Test
    public void disconnectSignal() {
        var success = new AtomicBoolean(true);
        Application app = new Application("test.id1", ApplicationFlags.DEFAULT_FLAGS);
        SignalConnection<GObject.Notify> signal = app.onNotify("application-id", paramSpec -> {
            success.set(false);
        });
        signal.disconnect();
        assertFalse(signal.isConnected());
        app.setApplicationId("test.id2");
        assertTrue(success.get());
    }

    @Test
    public void blockUnblockSignal() {
        var success = new AtomicBoolean(true);
        Application app = new Application("test.id1", ApplicationFlags.DEFAULT_FLAGS);
        SignalConnection<GObject.Notify> signal = app.onNotify("application-id", paramSpec -> {
            success.set(false);
        });
        signal.block();
        app.setApplicationId("test.id2");
        assertTrue(success.get());
        signal.unblock();
        app.setApplicationId("test.id3");
        assertFalse(success.get());
    }
}
