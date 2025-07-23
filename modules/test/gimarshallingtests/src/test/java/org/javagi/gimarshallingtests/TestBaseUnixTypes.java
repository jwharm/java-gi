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

package org.javagi.gimarshallingtests;

import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestBaseUnixTypes {

    // time_t

    @Test
    void time_tReturn() {
        assertEquals(1234567890L, timeTReturn());
    }

    @Test
    void time_tIn() {
        timeTIn(1234567890L);
    }

    @Test
    void time_tOut() {
        var v = new Out<>(0L);
        timeTOut(v);
        assertEquals(1234567890L, v.get());
    }

    @Test
    void time_tOutUninitialized() {
        var v = new Out<>(0L);
        assertFalse(timeTOutUninitialized(v));
        assertEquals(0L, v.get());
    }

    @Test
    void time_tInOut() {
        var v = new Out<>(1234567890L);
        timeTInout(v);
        assertEquals(0L, v.get());
    }

    // off_t

    @Test
    void off_tReturn() {
        assertEquals(1234567890L, offTReturn());
    }

    @Test
    void off_tIn() {
        offTIn(1234567890L);
    }

    @Test
    void off_tOut() {
        var v = new Out<>(0L);
        offTOut(v);
        assertEquals(1234567890L, v.get());
    }

    @Test
    void off_tOutUninitialized() {
        var v = new Out<>(0L);
        assertFalse(offTOutUninitialized(v));
        assertEquals(0L, v.get());
    }

    @Test
    void off_tInOut() {
        var v = new Out<>(1234567890L);
        offTInout(v);
        assertEquals(0L, v.get());
    }

    // dev_t

    /*
    Disable for now, this causes a compile error in CI that I suspect is
    caused by an older version of g-ir-scanner

    @Test
    void dev_tReturn() {
        assertEquals(1234567890L, devTReturn());
    }

    @Test
    void dev_tIn() {
        devTIn(1234567890L);
    }

    @Test
    void dev_tOut() {
        var v = new Out<>(0L);
        devTOut(v);
        assertEquals(1234567890L, v.get());
    }

    @Test
    void dev_tOutUninitialized() {
        var v = new Out<>(0L);
        assertFalse(devTOutUninitialized(v));
        assertEquals(0L, v.get());
    }

    @Test
    void dev_tInOut() {
        var v = new Out<>(1234567890L);
        devTInout(v);
        assertEquals(0L, v.get());
    }

    */

    // gid_t

    @Test
    void gid_tReturn() {
        assertEquals(65534, gidTReturn());
    }

    @Test
    void gid_tIn() {
        gidTIn(65534);
    }

    @Test
    void gid_tOut() {
        var v = new Out<>(0);
        gidTOut(v);
        assertEquals(65534, v.get());
    }

    @Test
    void gid_tOutUninitialized() {
        var v = new Out<>(0);
        assertFalse(gidTOutUninitialized(v));
        assertEquals(0, v.get());
    }

    @Test
    void gid_tInOut() {
        var v = new Out<>(65534);
        gidTInout(v);
        assertEquals(0, v.get());
    }

    // pid_t

    /*
    Disable for now, this causes a compile error in CI that I suspect is
    caused by an older version of g-ir-scanner

    @Test
    void pid_tReturn() {
        assertEquals(12345, pidTReturn());
    }

    @Test
    void pid_tIn() {
        pidTIn(12345);
    }

    @Test
    void pid_tOut() {
        var v = new Out<>(0);
        pidTOut(v);
        assertEquals(12345, v.get());
    }

    @Test
    void pid_tOutUninitialized() {
        var v = new Out<>(0);
        assertFalse(pidTOutUninitialized(v));
        assertEquals(0, v.get());
    }

    @Test
    void pid_tInOut() {
        var v = new Out<>(12345);
        pidTInout(v);
        assertEquals(0, v.get());
    }

    */

    // socklen_t

    @Test
    void socklen_tReturn() {
        assertEquals(123, socklenTReturn());
    }

    @Test
    void socklen_tIn() {
        socklenTIn(123);
    }

    @Test
    void socklen_tOut() {
        var v = new Out<>(0);
        socklenTOut(v);
        assertEquals(123, v.get());
    }

    @Test
    void socklen_tOutUninitialized() {
        var v = new Out<>(0);
        assertFalse(socklenTOutUninitialized(v));
        assertEquals(0, v.get());
    }

    @Test
    void socklen_tInOut() {
        var v = new Out<>(123);
        socklenTInout(v);
        assertEquals(0, v.get());
    }

    // uid_t

    @Test
    void uid_tReturn() {
        assertEquals(65534, uidTReturn());
    }

    @Test
    void uid_tIn() {
        uidTIn(65534);
    }

    @Test
    void uid_tOut() {
        var v = new Out<>(0);
        uidTOut(v);
        assertEquals(65534, v.get());
    }

    @Test
    void uid_tOutUninitialized() {
        var v = new Out<>(0);
        assertFalse(uidTOutUninitialized(v));
        assertEquals(0, v.get());
    }

    @Test
    void uid_tInOut() {
        var v = new Out<>(65534);
        uidTInout(v);
        assertEquals(0, v.get());
    }
}
