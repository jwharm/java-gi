package io.github.jwharm.javagi.test.gio;

import org.gnome.gio.Application;
import org.gnome.glib.GLib;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test calling GLib.idleAdd(...callback...) from another thread. The upcall
 * stub for the callback will be freed from the GLib main thread.
 */
public class IdleCallbackTest {

    @Test
    void idleCallbackTest() {
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
}
