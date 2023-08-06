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
