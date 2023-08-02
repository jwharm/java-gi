package io.github.jwharm.javagi.test.glib;

import io.github.jwharm.javagi.types.Properties;

import org.gnome.gio.Application;
import org.gnome.gio.ApplicationFlags;
import org.gnome.gobject.GObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the patched GObject methods to get and set properties,
 * and construct new GObject instances with properties
 */
public class PropertiesTest {

    @Test
    public void getProperty() {
        Application app = new Application("io.github.jwharm.javagi.test.Application", ApplicationFlags.DEFAULT_FLAGS);
        String applicationId = (String) app.getProperty("application-id");
        assertEquals("io.github.jwharm.javagi.test.Application", applicationId);
    }

    @Test
    public void setProperty() {
        Application app = new Application("io.github.jwharm.javagi.test.Application", ApplicationFlags.DEFAULT_FLAGS);
        app.setProperty("application-id", "my.example.Application");
        String applicationId = (String) Properties.getProperty(app, "application-id");
        assertEquals("my.example.Application", applicationId);
    }

    @Test
    public void newGObjectWithProperties() {
        Application app = GObject.newInstance(
                Application.getType(),
                "application-id", "io.github.jwharm.javagi.test.Application",
                "flags", ApplicationFlags.DEFAULT_FLAGS);
        String applicationId = (String) Properties.getProperty(app, "application-id");
        assertEquals("io.github.jwharm.javagi.test.Application", applicationId);
    }
}
