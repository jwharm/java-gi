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
import org.gnome.gtk.Label;
import org.gnome.gtk.ApplicationWindow;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that a @GtkChild field in a Gtk template class is setup
 */
public class TemplateChildTest {

    @Test
    public void testGtkChild() {
        // Register gresource bundle
        Resource resource = null;
        try {
            resource = Resource.load("src/test/resources/test.gresource");
        } catch (GErrorException e) {
            fail(e);
        }
        resource.resourcesRegister();

        // New Gtk application
        Application app = new Application(TemplateChildTest.class.getName(), ApplicationFlags.DEFAULT_FLAGS);
        app.onActivate(() -> {
            // New TestWindow (defined below)
            TestWindow tw = GObject.newInstance(TestWindow.gtype);

            // Check that the label field is set to the value from the ui file
            assertEquals(tw.label.getLabel(), "Test Label");

            app.quit();
        });
        app.run(null);
    }

    @GtkTemplate(name="ChildTestWindow", ui="/io/github/jwharm/javagi/gtk/TemplateChildTest.ui")
    public static class TestWindow extends ApplicationWindow {
        public static Type gtype = Types.register(TestWindow.class);
        public TestWindow(MemorySegment address ){
            super(address);
        }

        @GtkChild
        public Label label;
    }
}
