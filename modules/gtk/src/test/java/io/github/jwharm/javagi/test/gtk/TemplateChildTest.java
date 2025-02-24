/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 Jan-Willem Harmannij
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package io.github.jwharm.javagi.test.gtk;

import io.github.jwharm.javagi.base.GErrorException;
import io.github.jwharm.javagi.gobject.annotations.InstanceInit;
import io.github.jwharm.javagi.gobject.annotations.RegisteredType;
import io.github.jwharm.javagi.gobject.types.Types;
import io.github.jwharm.javagi.gtk.annotations.GtkChild;
import io.github.jwharm.javagi.gtk.annotations.GtkTemplate;
import org.gnome.gio.Resource;
import org.gnome.gtk.Application;
import org.gnome.gtk.Button;
import org.gnome.gtk.Label;
import org.gnome.gtk.ApplicationWindow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.foreign.MemorySegment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that a @GtkChild field in a Gtk template class is set up
 */
@Isolated
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

        // Make sure that the TestButton class is registered as a GType.
        // Otherwise, the "button" field can't be setup by GtkBuilder.
        Types.register(TestButton.class);

        // New Gtk application
        Application app = new Application(TemplateChildTest.class.getName());
        app.onActivate(() -> {
            // New TestWindow (defined below)
            TestWindow tw = new TestWindow();

            // Check that the label field is set to the value from the ui file
            assertEquals("Test Label", tw.button.getLabel());

            // Check that Java instance state is preserved
            assertTrue(tw.button.initialized);

            // Check that the "namedLabel" field (referring to the "label"
            // element in the XML using the annotation parameter "name", is set
            // to the expected value
            TestWindow tw2 = new TestWindow();
            assertEquals("Second Label", tw2.namedLabel.getLabel());

            app.quit();
        });
        app.run(null);
    }

    @GtkTemplate(name="ChildTestWindow", ui="/io/github/jwharm/javagi/gtk/TemplateChildTest.ui")
    public static class TestWindow extends ApplicationWindow {
        public TestWindow() {
            super();
        }

        @GtkChild
        public TestButton button;

        @GtkChild(name="label2")
        public Label namedLabel;
    }

    @RegisteredType(name="TestButton")
    public static class TestButton extends Button {
        public TestButton(MemorySegment address) {
            super(address);
        }

        public boolean initialized = false;

        @InstanceInit
        public void init() {
            this.initialized = true;
        }
    }
}
