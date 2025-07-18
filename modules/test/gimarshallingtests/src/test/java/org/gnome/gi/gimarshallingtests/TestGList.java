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
import org.javagi.base.TransferOwnership;
import org.javagi.interop.Interop;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.gnome.gi.gimarshallingtests.GIMarshallingTests.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestGList {
    private final List<String> TEST_UTF8_LIST = List.of("0", "1", "2");
    private final List<String> TEST_UTF8_LIST_OUT = List.of("-2", "-1", "0", "1");

    @Test
    void intNoneReturn() {
        List<Integer> list = glistIntNoneReturn();
        assertIterableEquals(List.of(-1, 0, 1, 2), list);
    }

    @Test
    void uint32NoneReturn() {
        List<Integer> list = glistUint32NoneReturn();
        assertIterableEquals(List.of(0, -1), list);
    }

    @Test
    void utf8NoneReturn() {
        List<String> list = glistUtf8NoneReturn();
        assertIterableEquals(TEST_UTF8_LIST, list);
    }

    @Test
    void utf8ContainerReturn() {
        List<String> list = glistUtf8ContainerReturn();
        assertIterableEquals(TEST_UTF8_LIST, list);
    }

    @Test
    void utf8FullReturn() {
        List<String> list = glistUtf8FullReturn();
        assertIterableEquals(TEST_UTF8_LIST, list);
    }

    @Test
    void intNoneIn() {
        var list = new org.gnome.glib.List<>(pointer -> (int) pointer.address(), null, TransferOwnership.CONTAINER);
        list.addAll(List.of(-1, 0, 1, 2));
        glistIntNoneIn(list);
    }

    @Test
    void uint32NoneIn() {
        var list = new org.gnome.glib.List<>(pointer -> (int) pointer.address(), null, TransferOwnership.CONTAINER);
        list.addAll(List.of(0, -1));
        glistUint32NoneIn(list);
    }

    @Test
    void utf8NoneIn() {
        var list = new org.gnome.glib.List<>(Interop::getStringFrom, null, TransferOwnership.FULL);
        list.addAll(TEST_UTF8_LIST);
        glistUtf8NoneIn(list);
    }

    @Test
    void utf8ContainerIn() {
        var list = new org.gnome.glib.List<>(Interop::getStringFrom, null, TransferOwnership.NONE);
        list.addAll(TEST_UTF8_LIST);
        glistUtf8ContainerIn(list);
    }

    @Test
    void utf8FullIn() {
        var list = new org.gnome.glib.List<>(Interop::getStringFrom, null, TransferOwnership.NONE);
        list.addAll(TEST_UTF8_LIST);
        glistUtf8FullIn(list);
    }

    @Test
    void utf8NoneOut() {
        var v = new Out<org.gnome.glib.List<String>>();
        glistUtf8NoneOut(v);
        assertIterableEquals(TEST_UTF8_LIST, v.get());
    }

    @Test
    void utf8NoneOutUninitialized() {
        var v = new Out<org.gnome.glib.List<String>>();
        assertFalse(glistUtf8NoneOutUninitialized(v));
        assertIterableEquals(Collections.emptyList(), v.get());
    }

    @Test
    void utf8ContainerOut() {
        var v = new Out<org.gnome.glib.List<String>>();
        glistUtf8ContainerOut(v);
        assertIterableEquals(TEST_UTF8_LIST, v.get());
    }

    @Test
    void utf8ContainerOutUninitialized() {
        var v = new Out<org.gnome.glib.List<String>>();
        assertFalse(glistUtf8ContainerOutUninitialized(v));
        assertIterableEquals(Collections.emptyList(), v.get());
    }

    @Test
    void utf8FullOut() {
        var v = new Out<org.gnome.glib.List<String>>();
        glistUtf8FullOut(v);
        assertIterableEquals(TEST_UTF8_LIST, v.get());
    }

    @Test
    void utf8FullOutUninitialized() {
        var v = new Out<org.gnome.glib.List<String>>();
        assertFalse(glistUtf8FullOutUninitialized(v));
        assertIterableEquals(Collections.emptyList(), v.get());
    }

    @Test
    void utf8NoneInout() {
        var list = new org.gnome.glib.List<>(Interop::getStringFrom, null, TransferOwnership.FULL);
        list.addAll(TEST_UTF8_LIST);
        var v = new Out<>(list);
        glistUtf8NoneInout(v);
        assertIterableEquals(TEST_UTF8_LIST_OUT, v.get());
    }

    @Test
    void utf8ContainerInout() {
        var list = new org.gnome.glib.List<>(Interop::getStringFrom, null, TransferOwnership.FULL);
        list.addAll(TEST_UTF8_LIST);
        var v = new Out<>(list);
        glistUtf8ContainerInout(v);
        assertIterableEquals(TEST_UTF8_LIST_OUT, v.get());
    }

    @Test
    void utf8FullInout() {
        var list = new org.gnome.glib.List<>(Interop::getStringFrom, null, TransferOwnership.FULL);
        list.addAll(TEST_UTF8_LIST);
        var v = new Out<>(list);
        glistUtf8FullInout(v);
        assertIterableEquals(TEST_UTF8_LIST_OUT, v.get());
    }
}
