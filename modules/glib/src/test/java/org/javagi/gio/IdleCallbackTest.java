package org.javagi.gio;

import org.gnome.gio.Application;
import org.gnome.glib.GLib;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class IdleCallbackTest {

    /**
     * Test calling GLib.idleAdd(...callback...) from another thread. The upcall
     * stub for the callback will be freed from the GLib main thread.
     */
    @Test
    void idleCallbackFromOtherThread() {
        var success = new AtomicBoolean(false);
        var app = new Application("javagi.test.IdleCallbackTest");
        app.onActivate(() -> {
            var thread = new Thread(() -> GLib.idleAdd(GLib.PRIORITY_DEFAULT, () -> {
                success.set(true);
                return GLib.SOURCE_REMOVE;
            }));
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        });
        app.run(null);
        assertTrue(success.get());
    }

    /**
     * Test custom Java wrapper for GLib.idleAddOnce
     */
    @Test
    void idleAddOnce() {
        var success = new AtomicBoolean(false);
        var app = new Application("javagi.test.IdleAddOnce");
        app.onActivate(() -> {
            GLib.idleAddOnce(() -> success.set(true));
            // Wait for the timeout to run
            checkAndSleep(success);
        });
        app.run(null);
        assertTrue(success.get());
    }

    /**
     * Test custom Java wrapper for GLib.timeoutAddOnce
     */
    @Test
    void timeoutAddOnce() {
        var success = new AtomicBoolean(false);
        var app = new Application("javagi.test.timeoutAddOnce");
        app.onActivate(() -> {
            GLib.timeoutAddOnce(0, () -> success.set(true));
            // Wait for the timeout to run
            checkAndSleep(success);
        });
        app.run(null);
        assertTrue(success.get());
    }

    /**
     * Test custom Java wrapper for GLib.timeoutAddSecondsOnce
     */
    @Test
    void timeoutAddSecondsOnce() {
        var success = new AtomicBoolean(false);
        var app = new Application("javagi.test.timeoutAddSecondsOnce");
        app.onActivate(() -> {
            GLib.timeoutAddSecondsOnce(0, () -> success.set(true));
            // Wait for the timeout to run
            checkAndSleep(success);
        });
        app.run(null);
        assertTrue(success.get());
    }

    // Sleep for slightly more than one second in 100ms steps, but return
    // early when the "status" variable has been set to "true".
    private static void checkAndSleep(AtomicBoolean status) {
        try {
            for (int i = 0; i < 11; i++) {
                if (status.get())
                    return;
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
