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
import org.gnome.gtk.ApplicationWindow;
import org.gnome.gtk.Button;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.lang.foreign.MemorySegment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that a signal defined in a Gtk template class calls the specified Java method
 */
@Isolated
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
        public static Type gtype = TemplateTypes.register(TestWindow.class);
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
