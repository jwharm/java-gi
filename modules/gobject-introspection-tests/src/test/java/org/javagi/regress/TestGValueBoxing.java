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

package org.javagi.regress;

import org.gnome.glib.Date;
import org.gnome.glib.DateMonth;
import org.gnome.gobject.Value;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.regress.Regress.*;
import static org.javagi.gobject.ValueUtil.valueToObject;
import static org.junit.jupiter.api.Assertions.*;

public class TestGValueBoxing {
    @Test
    void withDate() {
        Value value = testDateInGvalue();
        Date date = (Date) valueToObject(value);
        assertNotNull(date);
        assertEquals((short) 1984, date.getYear().getValue());
        assertEquals(DateMonth.DECEMBER, date.getMonth());
        assertEquals((byte) 5, date.getDay().getValue());
    }

    @Test
    void withStrv() {
        Value value = testStrvInGvalue();
        String[] strv = (String[]) valueToObject(value);
        assertArrayEquals(new String[] {"one", "two", "three"}, strv);
    }

    @Test
    void withStrvNull() {
        Value value = testNullStrvInGvalue();
        String[] strv = (String[]) valueToObject(value);
        assertNull(strv);
    }
}
