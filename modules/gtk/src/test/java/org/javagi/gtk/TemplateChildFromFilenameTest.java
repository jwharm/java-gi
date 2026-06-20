/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2026 Jan-Willem Harmannij
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

package org.javagi.gtk;

import org.gnome.gtk.Application;
import org.gnome.gtk.ApplicationWindow;
import org.gnome.gtk.Label;
import org.javagi.gtk.annotations.GtkChild;
import org.javagi.gtk.annotations.GtkTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test that a Gtk template class is set up from a filesystem path
 */
@Isolated
public class TemplateChildFromFilenameTest {

    @Test
    public void testGtkChild() {
        // New Gtk application
        Application app = new Application(TemplateChildFromFilenameTest.class.getName());
        app.onActivate(() -> {
            TestWindow tw = new TestWindow();
            assertEquals("Label text", tw.label.getLabel());
            app.quit();
        });
        app.run(null);
    }

    @GtkTemplate(name="TestWindowOnFileSystem", ui="src/test/resources/TemplateTestOnFileSystem.ui")
    public static class TestWindow extends ApplicationWindow {
        @GtkChild
        public Label label;
    }
}
