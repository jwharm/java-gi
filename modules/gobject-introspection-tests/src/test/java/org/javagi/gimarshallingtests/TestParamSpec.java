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

import org.gnome.gobject.GObjects;
import org.gnome.gobject.ParamFlags;
import org.gnome.gobject.ParamSpec;
import org.javagi.base.Out;
import org.javagi.gobject.types.Types;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestParamSpec {
    @Test
    void inBool() {
        var pspec = GObjects.paramSpecBoolean("mybool", "My Bool", "My boolean property", true, ParamFlags.READWRITE);
        paramSpecInBool(pspec);
    }

    @Test
    void return_() {
        var pspec = paramSpecReturn();
        assertNotNull(pspec);
        assertEquals("test-param", pspec.getName());
        assertEquals("test", pspec.getNick());
        assertEquals("This is a test", pspec.getBlurb());
        assertEquals(Types.STRING, pspec.getDefaultValue().readGType());
        assertEquals("42", pspec.getDefaultValue().getString());
    }

    @Test
    void out() {
        var v = new Out<ParamSpec>();
        paramSpecOut(v);
        var pspec = v.get();
        assertEquals("test-param", pspec.getName());
        assertEquals("test", pspec.getNick());
        assertEquals("This is a test", pspec.getBlurb());
        assertEquals(Types.STRING, pspec.getDefaultValue().readGType());
        assertEquals("42", pspec.getDefaultValue().getString());
    }

    @Test
    void outUninitialized() {
        assertDoesNotThrow(() -> paramSpecOutUninitialized(null));
        var v = new Out<ParamSpec>();
        assertDoesNotThrow(() -> paramSpecOutUninitialized(v));
    }
}
