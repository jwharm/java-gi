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

package org.javagi.gio;

import org.gnome.glib.GLib;
import org.gnome.glib.NumberParserError;
import org.gnome.glib.NumberParserException;
import org.javagi.base.GErrorException;
import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test specialized exceptions: g_ascii_string_to_signed throws a
 * NumberParserError. The exact error domain is not known from introspection,
 * so in Java the method signature throws GErrorException, but the actual
 * exception will be a NumberParserException.
 */
public class ExceptionTest {
    @Test
    public void testNumberParserError() {
        try {
            GLib.asciiStringToSigned("invalid", 10, 0, 100, new Out<>());
            fail("Expected NumberParserException to be thrown");
        } catch (NumberParserException npe) {
            assertEquals(NumberParserError.INVALID, npe.getEnum());
        } catch (GErrorException e) {
            fail("Expected NumberParserException, but GErrorException was thrown");
        }
    }
}
