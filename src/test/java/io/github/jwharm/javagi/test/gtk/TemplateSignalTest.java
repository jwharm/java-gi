package io.github.jwharm.javagi.test.gtk;

import io.github.jwharm.javagi.base.GErrorException;
import io.github.jwharm.javagi.gtk.annotations.GtkChild;
import io.github.jwharm.javagi.gtk.annotations.GtkTemplate;
import io.github.jwharm.javagi.gtk.types.Types;
import org.gnome.gio.ApplicationFlags;
import org.gnome.gio.Resource;
import org.gnome.glib.Type;
import org.gnome.gobject.GObject;
import org.gnome.gtk.Application;
import org.gnome.gtk.ApplicationWindow;
import org.gnome.gtk.Button;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that a signal defined in a Gtk template class calls the specified Java method
 */
public class TemplateSignalTest {

    @Test
    public void testSignalConnection() {
        // Register gresource bundle
        Resource resource = null;
        try {
            resource = Resource.load("src/test/resources/test.gresource");
        } catch (GErrorException e) {
            fail(e);
        }
        resource.resourcesRegister();

        // New Gtk application
        Application app = new Application(TemplateSignalTest.class.getName(), ApplicationFlags.DEFAULT_FLAGS);
        app.onActivate(() -> {
            // New TestWindow (defined below)
            TestWindow tw = GObject.newInstance(TestWindow.gtype);

            // Emit the "clicked" signal that should be connected to the `buttonClicked()` java method
            tw.button.emitClicked();

            app.quit();
            assertTrue(tw.signalReceived);
        });
        app.run(null);
    }

    @GtkTemplate(name="SignalTestWindow", ui="/io/github/jwharm/javagi/gtk/TemplateSignalTest.ui")
    public static class TestWindow extends ApplicationWindow {
        public static Type gtype = Types.register(TestWindow.class);
        public TestWindow(MemorySegment address ){
            super(address);
        }

        @GtkChild
        public Button button;

        public boolean signalReceived = false;

        // Invoked by the <signal> connection defined in the ui file
        public void buttonClicked() {
            signalReceived = true;
        }
    }
}
