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

import io.github.jwharm.cairobindings.MemoryCleaner;
import org.freedesktop.cairo.*;
import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.gnome.gi.regress.Regress.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestCairo {
    @Test
    void contextFullReturn() {
        assertNotNull(testCairoContextFullReturn());
    }

    @Test
    void contextNoneReturn() {
        var cr = testCairoContextNoneReturn();
        assertNotNull(cr);
        // We have to do this manually when using the cairo bindings like this
        MemoryCleaner.yieldOwnership(cr.handle());
    }

    @Test
    void contextNoneIn() throws IOException {
        try (var surface = ImageSurface.create(Format.ARGB32, 10, 10)) {
            var cr = Context.create(surface);
            testCairoContextNoneIn(cr);
        }
    }

    @Test
    void surfaceNoneReturn() {
        var surface = testCairoSurfaceNoneReturn();
        assertNotNull(surface);
        assertEquals(SurfaceType.IMAGE, surface.getSurfaceType());
        // We have to do this manually when using the cairo bindings like this
        MemoryCleaner.yieldOwnership(surface.handle());
    }

    @Test
    void surfaceFullReturn() {
        var surface = testCairoSurfaceFullReturn();
        assertNotNull(surface);
        assertEquals(SurfaceType.IMAGE, surface.getSurfaceType());
    }

    @Test
    void surfaceNoneIn() {
        try (var surface = ImageSurface.create(Format.ARGB32, 10, 10)) {
            testCairoSurfaceNoneIn(surface);
        }
    }

    @Test
    void surfaceFullOut() {
        var v = new Out<Surface>();
        testCairoSurfaceFullOut(v);
        assertNotNull(v.get());
        assertEquals(SurfaceType.IMAGE, v.get().getSurfaceType());
    }
}
