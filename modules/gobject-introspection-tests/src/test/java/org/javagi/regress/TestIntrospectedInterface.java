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
import org.gnome.gobject.GObject;
import org.javagi.gobject.annotations.Property;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class TestIntrospectedInterface {
    public static class Implementor extends GObject implements TestInterface {
        @Property
        public int getNumber() {
            return 5;
        }
    }

    @Test
    void correctlyEmitsInterfaceSignals() {
        // Explicitly call Regress initalization, or else TestInterface is not registered
        Regress.javagi$ensureInitialized();

        var hasBeenCalled = new AtomicBoolean(false);
        var obj = new Implementor();
        obj.onInterfaceSignal(_ -> hasBeenCalled.set(true));
        obj.emitInterfaceSignal(MemorySegment.NULL);
        assertTrue(hasBeenCalled.get());
    }
}
