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

import org.gnome.gi.regress.TestAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestClassWithActionSignals {
    TestAction o;

    @BeforeEach
    void init() {
        o = new TestAction();
    }

    @Test
    void returnsNewObject() {
        Object otherObj = o.emit("action");
        assertInstanceOf(TestAction.class, otherObj);
        assertNotEquals(o, otherObj);

        TestAction otherObj2 = o.emitAction();
        assertNotEquals(o, otherObj2);
    }

    @Test
    void returnsNull() {
        assertNull(o.emit("action2"));
        assertNull(o.emitAction2());
    }
}
