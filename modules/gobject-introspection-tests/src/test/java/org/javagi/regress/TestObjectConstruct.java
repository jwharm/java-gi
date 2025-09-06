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

import org.gnome.gi.regress.*;
import org.javagi.base.GErrorException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestObjectConstruct {
    @Test
    void createWithNew() {
        TestObj object2 = new TestObj();
        assertNotNull(object2);
    }

    @Test
    void createWithBuilder() {
        TestObj object1 = TestObj.builder()
                .setInt(42)
                .setFloat(3.1416f)
                .setDouble(2.71828)
                .build();
        assertNotNull(object1);
        assertEquals(42, object1.getProperty("int"));
        assertEquals(3.1416f, object1.getProperty("float"));
        assertEquals(2.71828, object1.getProperty("double"));
    }

    @Test
    void createWithFactoryMethod() {
        // Function annotated with (constructor)
        TestObj object3 = TestObj.constructor();
        assertNotNull(object3);

        // Static method
        try {
            TestObj object4 = TestObj.fromFile("/enoent");
            assertNotNull(object4);
        } catch (GErrorException ignored) {
            fail();
        }
    }
}
