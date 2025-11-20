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

import org.gnome.gi.gimarshallingtests.Flags;
import org.gnome.gi.gimarshallingtests.NoTypeFlags;
import org.javagi.base.Out;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestFlags {
    @Test
    void returnv() {
        assertIterableEquals(Set.of(Flags.VALUE2), Flags.returnv());
    }

    @Test
    void in() {
        Flags.VALUE2.in();
    }

    @Test
    void inZero() {
        /* The "Flags" type doesn't have a member with value 0, and the
         * "inZero()" function is an instance method of a Flags member.
         * Therefore, this doesn't work in Java-GI. It does work for functions
         * that accept flags parameters, because there we can pass an empty set
         * (see noTypeInZero() below for a concrete example).
         */
        Flags.of(0).stream().findAny().ifPresent(Flags::inZero);
    }

    @Test
    void out() {
        var v = new Out<Set<Flags>>();
        Flags.out(v);
        assertIterableEquals(Set.of(Flags.VALUE2), v.get());
    }

    @Test
    void outUninitialized() {
        var v = new Out<Set<Flags>>();
        assertFalse(Flags.outUninitialized(v));
    }

    @Test
    void inout() {
        var v = new Out<>(Set.of(Flags.VALUE2));
        Flags.inout(v);
        assertIterableEquals(Set.of(Flags.VALUE1), v.get());
    }

    @Test
    void noTypeReturnv() {
        assertIterableEquals(Set.of(NoTypeFlags.VALUE2), noTypeFlagsReturnv());
    }

    @Test
    void noTypeIn() {
        noTypeFlagsIn(NoTypeFlags.VALUE2);
        noTypeFlagsIn(Set.of(NoTypeFlags.VALUE2));
    }

    @Test
    void noTypeInZero() {
        noTypeFlagsInZero(EnumSet.noneOf(NoTypeFlags.class));
    }

    @Test
    void noTypeOut() {
        var v = new Out<Set<NoTypeFlags>>();
        noTypeFlagsOut(v);
        assertIterableEquals(Set.of(NoTypeFlags.VALUE2), v.get());
    }

    @Test
    void noTypeOutUninitialized() {
        var v = new Out<Set<NoTypeFlags>>();
        assertFalse(noTypeFlagsOutUninitialized(v));
    }

    @Test
    void noTypeInout() {
        var v = new Out<>(Set.of(NoTypeFlags.VALUE2));
        noTypeFlagsInout(v);
        assertIterableEquals(Set.of(NoTypeFlags.VALUE1), v.get());
    }
}
