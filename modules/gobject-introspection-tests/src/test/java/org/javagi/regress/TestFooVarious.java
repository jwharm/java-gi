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

import org.gnome.gi.regress.FooObject;
import org.gnome.gi.utility.*;
import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestFooVarious {
    @Test
    void init() {
        assertEquals(0x1138, fooInit());
    }

    @Test
    void initArgv() {
        assertEquals(0x1138, fooInitArgv(null));
        assertEquals(0x1138, fooInitArgv(new String[] {}));
    }

    @Test
    void initArgvAddress() {
        var args = new String[] {"--num", "5", "--no-florp"};
        var out = new Out<>(args);
        assertEquals(0x1138, fooInitArgvAddress(out));
        assertArrayEquals(args, out.get());
    }

    @Test
    void notAConstructorNew() {
        assertNull(fooNotAConstructorNew());
    }

    @Test
    void methodExternalReferences() {
        var o = new UtilityObject();
        var s = new Struct();
        fooMethodExternalReferences(o, EnumType.C, FlagType.B, s);
    }

    @Test
    void objectGlobalMethod() {
        var o = new UtilityObject();
        FooObject.aGlobalMethod(o);
    }
}
