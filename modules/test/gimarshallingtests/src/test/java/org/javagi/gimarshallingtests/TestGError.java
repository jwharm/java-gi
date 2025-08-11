/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2025 Jan-Willem Harmannij
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

package org.javagi.gimarshallingtests;

import org.gnome.gi.gimarshallingtests.GIMarshallingTests;
import org.gnome.glib.GError;
import org.gnome.glib.GLib;
import org.javagi.base.GErrorException;
import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestGError {
    @Test
    void new_() {
        assertThrows(GErrorException.class, GIMarshallingTests::gerror);
    }

    @Test
    void arrayIn() {
        try {
            gerrorArrayIn(new int[] {-1, 0, 1, 2});
            fail();
        } catch (GErrorException e) {
            assertEquals(GLib.quarkFromString(CONSTANT_GERROR_DOMAIN), e.getDomain());
            assertEquals(CONSTANT_GERROR_MESSAGE, e.getMessage());
        }
    }

    @Test
    void out() {
        var error = new Out<GError>();
        var debug = new Out<String>();
        gerrorOut(error, debug);
        assertNotNull(error.get());
        assertEquals(CONSTANT_GERROR_DEBUG_MESSAGE, debug.get());
    }

    @Test
    void outUninitialized() {
        assertThrows(NullPointerException.class, () -> gerrorOutUninitialized(null, null));
        var error = new Out<GError>();
        var debug = new Out<String>();
        assertDoesNotThrow(() -> gerrorOutUninitialized(error, debug));
    }

    @Test
    void outTransferNone() {
        var error = new Out<GError>();
        var debug = new Out<String>();
        gerrorOutTransferNone(error, debug);
        assertNotNull(error.get());
        assertEquals(CONSTANT_GERROR_DEBUG_MESSAGE, debug.get());
    }

    @Test
    void outTransferNoneUninitialized() {
        assertThrows(NullPointerException.class, () -> gerrorOutTransferNoneUninitialized(null, null));
        var error = new Out<GError>();
        var debug = new Out<String>();
        assertDoesNotThrow(() -> gerrorOutTransferNoneUninitialized(error, debug));
    }

    @Test
    void return_() {
        assertNotNull(gerrorReturn());
    }
}
