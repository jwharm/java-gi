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
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Miscellanious tests
 */
public class TestMisc {
    @Test
    void structWithFixedLengthArrayField() {
        var o = new AnnotationObject();
        var objects = new AnnotationObject[] {o, o, o, o, o, o, o, o, o, o};
        try (Arena arena = Arena.ofConfined()) {
            var s = new AnnotationStruct(objects, arena);
            var result = s.readObjects();
            assertArrayEquals(objects, result);
        }
    }

    @Test
    void writeToArrayField() {
        try (Arena arena = Arena.ofConfined()) {
            var f = new AnnotationFields();
            f.writeField1(42);
            f.writeField4(43);
            byte[] bytesIn = new byte[] {104, 105, 106, 107};
            f.writeArr(bytesIn, arena);
            f.writeLen(4); // Array length field is not updated automatically
            byte[] bytesOut = f.readArr();
            assertArrayEquals(bytesIn, bytesOut);
        }
    }

    @Test
    void calculatedConstants() {
        assertEquals(100, ANNOTATION_CALCULATED_DEFINE);
        assertEquals(1000000, ANNOTATION_CALCULATED_LARGE_DIV);
        assertEquals(10000000000L, ANNOTATION_CALCULATED_LARGE);
    }
}
