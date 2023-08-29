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

package io.github.jwharm.javagi.test.gio;

import io.github.jwharm.javagi.gobject.SignalConnection;
import org.gnome.gio.Application;
import org.gnome.gio.ApplicationFlags;
import org.gnome.gobject.GObject;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test connecting a signal, and blocking/unblocking it
 */
public class SignalTest {

    @Test
    public void connectSignal() {
        var success = new AtomicBoolean(false);
        Application app = new Application("test.id1", ApplicationFlags.DEFAULT_FLAGS);
        SignalConnection<GObject.Notify> signal = app.onNotify("application-id", paramSpec -> {
            success.set(true);
        });
        assertTrue(signal.isConnected());
        app.setApplicationId("test.id2");
        assertTrue(success.get());
    }

    @Test
    public void disconnectSignal() {
        var success = new AtomicBoolean(true);
        Application app = new Application("test.id1", ApplicationFlags.DEFAULT_FLAGS);
        SignalConnection<GObject.Notify> signal = app.onNotify("application-id", paramSpec -> {
            success.set(false);
        });
        signal.disconnect();
        assertFalse(signal.isConnected());
        app.setApplicationId("test.id2");
        assertTrue(success.get());
    }

    @Test
    public void blockUnblockSignal() {
        var success = new AtomicBoolean(true);
        Application app = new Application("test.id1", ApplicationFlags.DEFAULT_FLAGS);
        SignalConnection<GObject.Notify> signal = app.onNotify("application-id", paramSpec -> {
            success.set(false);
        });
        signal.block();
        app.setApplicationId("test.id2");
        assertTrue(success.get());
        signal.unblock();
        app.setApplicationId("test.id3");
        assertFalse(success.get());
    }
}
