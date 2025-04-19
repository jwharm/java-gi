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

package io.github.jwharm.javagi.test.gio;

import io.github.jwharm.javagi.gobject.types.Properties;

import org.gnome.gio.Application;
import org.gnome.gio.ApplicationFlags;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the patched GObject methods to get and set properties,
 * and construct new GObject instances with properties
 */
public class PropertiesTest {

    @Test
    public void getProperty() {
        Application app = new Application("io.github.jwharm.javagi.test.Application");
        String applicationId = (String) app.getProperty("application-id");
        assertEquals("io.github.jwharm.javagi.test.Application", applicationId);
    }

    @Test
    public void setProperty() {
        Application app = new Application("io.github.jwharm.javagi.test.Application");
        app.setProperty("application-id", "my.example.Application");
        String applicationId = (String) Properties.getProperty(app, "application-id");
        assertEquals("my.example.Application", applicationId);
    }

    @Test
    public void newGObjectWithProperties() {
        Application app = new Application(
                "application-id", "io.github.jwharm.javagi.test.Application",
                "flags", ApplicationFlags.DEFAULT_FLAGS);
        String applicationId = (String) Properties.getProperty(app, "application-id");
        assertEquals("io.github.jwharm.javagi.test.Application", applicationId);
    }

    @Test
    public void builder() {
        AtomicBoolean notified = new AtomicBoolean(false);
        Application app = Application.builder()
                .setApplicationId("javagi.test.Application1")
                .setFlags(Set.of(ApplicationFlags.IS_SERVICE))
                .onNotify("application-id", _ -> notified.set(true))
                .build();

        // Assert that the properties are set
        assertEquals("javagi.test.Application1", app.getApplicationId());
        assertEquals(Set.of(ApplicationFlags.IS_SERVICE), app.getFlags());

        // Assert that the "notify" signal is connected
        // only for the specified property
        assertFalse(notified.get());
        app.setFlags(ApplicationFlags.DEFAULT_FLAGS);
        assertFalse(notified.get());
        app.setApplicationId("javagi.test.Application2");
        assertTrue(notified.get());
    }
}
