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

package org.gnome.gi.gimarshallingtests;

import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestBoolean {
    @Test
    void returnTrue() {
        assertTrue(booleanReturnTrue());
    }

    @Test
    void returnFalse() {
        assertFalse(booleanReturnFalse());
    }

    @Test
    void inTrue() {
        booleanInTrue(true);
    }

    @Test
    void inFalse() {
        booleanInFalse(false);
    }

    @Test
    void outTrue() {
        var v = new Out<Boolean>();
        booleanOutTrue(v);
        assertTrue(v.get());
    }

    @Test
    void outFalse() {
        var v = new Out<Boolean>();
        booleanOutFalse(v);
        assertFalse(v.get());
    }

    @Test
    void outUninitialized() {
        assertFalse(booleanOutUninitialized(null));
    }

    @Test
    void inOutTrueFalse() {
        var v = new Out<>(true);
        booleanInoutTrueFalse(v);
        assertFalse(v.get());
    }

    @Test
    void inOutFalseTrue() {
        var v = new Out<>(false);
        booleanInoutFalseTrue(v);
        assertTrue(v.get());
    }
}
