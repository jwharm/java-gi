/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.test.gobject;

import io.github.jwharm.javagi.gobject.annotations.RegisteredType;
import org.gnome.gobject.GObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the GObject property binding builder
 */
public class PropertyBindingTest {

    @Test
    void testPropertyBinding() {
        Speed kmh = new Speed(0.0);
        Speed mph = new Speed(0.0);

        kmh.<Double, Double>bindProperty("current-speed", mph, "current-speed")
                .bidirectional()
                .transformTo(s -> s * 0.6)
                .transformFrom(s -> s * 1.6)
                .build();

        kmh.setProperty("current-speed", 120.0);
        assertEquals(72.0, mph.getCurrentSpeed());
        mph.setProperty("current-speed", 70.0);
        assertEquals(112.0, kmh.getCurrentSpeed());
    }

    @Test
    void testArgumentValidation() {
        Speed kmh = new Speed(0.0);
        Speed mph = new Speed(0.0);

        try {
            kmh.<Double, Double>bindProperty("too-fast", mph, "too-fast")
                    .invertBoolean()
                    .transformTo(v -> v)
                    .build();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            kmh.<Double, Double>bindProperty("too-fast", mph, "too-fast")
                    .invertBoolean()
                    .transformFrom(v -> v)
                    .build();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        kmh.<Double, Double>bindProperty("too-fast", mph, "too-fast")
                .invertBoolean()
                .build();
        kmh.setProperty("too-fast", false);
        assertTrue((boolean) mph.getProperty("too-fast"));
    }

    @RegisteredType(name="Speed")
    public static class Speed extends GObject {
        private double currentSpeed;
        private final double limit = 70.0;

        public Speed(double currentSpeed) {
            super();
            setProperty("current-speed", currentSpeed);
        }

        public double getCurrentSpeed() {
            return currentSpeed;
        }

        public void setCurrentSpeed(double currentSpeed) {
            this.currentSpeed = currentSpeed;
        }

        public boolean getTooFast() {
            return currentSpeed > limit;
        }

        public void setTooFast(boolean tooFast) {
            if (tooFast)
                setProperty("current-speed", limit + 10.0);
            else
                setProperty("current-speed", limit - 10.0);
        }
    }
}
