/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
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
import io.github.jwharm.javagi.gtk.annotations.GtkChild;
import io.github.jwharm.javagi.gtk.annotations.GtkTemplate;
import io.github.jwharm.javagi.gtk.types.TemplateTypes;
import org.gnome.gio.ApplicationFlags;
import org.gnome.gio.Resource;
import org.gnome.gobject.Type;
import org.gnome.gobject.GObject;
import org.gnome.gtk.Application;
import org.gnome.gtk.Label;
import org.gnome.gtk.ApplicationWindow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.foreign.MemorySegment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that a @GtkChild field in a Gtk template class is setup
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

        // New Gtk application
        Application app = new Application(TemplateChildTest.class.getName(), ApplicationFlags.DEFAULT_FLAGS);
        app.onActivate(() -> {
            // New TestWindow (defined below)
            TestWindow tw = GObject.newInstance(TestWindow.gtype);

            // Check that the label field is set to the value from the ui file
            assertEquals(tw.label.getLabel(), "Test Label");

            // Check that the "namedLabel" field (referring to the "label"
            // element in the XML using the annotation parameter "name", is set
            // to the expected value
            TestWindow tw2 = GObject.newInstance(TestWindow.gtype);
            assertEquals(tw2.namedLabel.getLabel(), "Second Label");

            app.quit();
        });
        app.run(null);
    }

    @GtkTemplate(name="ChildTestWindow", ui="/io/github/jwharm/javagi/gtk/TemplateChildTest.ui")
    public static class TestWindow extends ApplicationWindow {
        public static Type gtype = TemplateTypes.register(TestWindow.class);
        public TestWindow(MemorySegment address ){
            super(address);
        }

        @GtkChild
        public Label label;

        @GtkChild(name="label2")
        public Label namedLabel;
    }
}
