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
import org.javagi.base.Out;
import org.javagi.base.TransferOwnership;
import org.javagi.interop.Interop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class TestAnnotationObject {
    AnnotationObject o;

    @BeforeEach
    void init() {
        o = new AnnotationObject();
    }

    @Test
    void deprecatedStringProperty() {
        assertNull(o.getProperty("string-property"));
        o.setProperty("string-property", "foo");
    }

    @Test
    void callbackProperty() {
        assertNull(o.getProperty("function-property"));
        o.setProperty("function-property", (Object) null);

        // We have to handle the lifetime of the callback manually
        try (Arena arena = Arena.ofConfined()) {
            AnnotationCallback cb = a -> a;
            MemorySegment ptr = cb.toCallback(arena);
            o.setProperty("function-property", ptr);
        }

        // This is easier, but uses the global arena
        AnnotationObject.builder().setFunctionProperty(a -> a).build();
    }

    @Test
    void tabProperty() {
        assertNull(o.getProperty("tab-property"));
        o.setProperty("tab-property", "\t");
    }

    @Test @SuppressWarnings("deprecation")
    void stringSignal() {
        var hasBeenCalled = new AtomicBoolean(false);
        o.onStringSignal(str -> {
            hasBeenCalled.set(true);
            assertEquals("foo", str);
        });
        o.emitStringSignal("foo");
        assertTrue(hasBeenCalled.get());
    }

    @Test
    void listSignal() {
        var hasBeenCalled = new AtomicBoolean(false);
        var input = List.of("foo", "bar");
        o.onListSignal(list -> {
            hasBeenCalled.set(true);
            assertIterableEquals(input, list);
        });
        var list = new org.gnome.glib.List<>(Interop::getStringFrom, null, TransferOwnership.NONE);
        list.addAll(input);
        o.emitListSignal(list);
        assertTrue(hasBeenCalled.get());
    }

    @Test
    void emptyDocArgument() {
        var hasBeenCalled = new AtomicBoolean(false);
        o.onDocEmptyArgParsing(arg -> {
            hasBeenCalled.set(true);
            assertEquals(MemorySegment.NULL, arg);
        });
        o.emitDocEmptyArgParsing(null);
        assertTrue(hasBeenCalled.get());
    }

    @Test
    void attributeSignal() {
        var hasBeenCalled = new AtomicBoolean(false);
        o.onAttributeSignal((s1, s2) -> {
            hasBeenCalled.set(true);
            assertEquals("foo", s1);
            assertEquals("bar", s2);
            return s1 + s2;
        });
        String result = o.emitAttributeSignal("foo", "bar");
        assertEquals("foobar", result);
        assertTrue(hasBeenCalled.get());
    }

    @Test
    void optionalInoutArgument() {
        // Arguments passed by pointer are not automatically handled.
        // Allocate the pointer manually.
        try (Arena arena = Arena.ofConfined()) {
            var ptr = arena.allocateFrom(ValueLayout.JAVA_INT, 2);
            assertEquals(2, o.in(ptr));
        }
    }

    @Test
    void method() {
        assertEquals(1, o.method());
    }

    @Test
    void out() {
        var out = new Out<Integer>();
        assertEquals(1, o.out(out));
        assertEquals(2, out.get());
    }

    @Test
    void inout() {
        var out = new Out<>(2);
        assertEquals(3, o.inout(out));
        assertEquals(3, out.get());
    }

    @Test
    void inout2() {
        var out = new Out<>(2);
        assertEquals(3, o.inout2(out));
        assertEquals(3, out.get());
    }

    @Test
    void inout3() {
        var out = new Out<>(1);
        assertEquals(2, o.inout3(out));
        assertEquals(1, out.get());
    }

    @Test
    void calleeOwns() {
        var out = new Out<GObject>();
        assertEquals(1, o.calleeowns(out));
        assertNull(out.get());
    }

    @Test
    void calleesOwns() {
        var out1 = new Out<GObject>();
        var out2 = new Out<GObject>();
        assertEquals(1, o.calleesowns(out1, out2));
        assertNull(out1.get());
        assertNull(out2.get());
    }

    @Test
    void getStrings() {
        assertIterableEquals(List.of("bar", "regress_annotation"), o.getStrings());
    }

    @Test
    void getHashtable() {
        var result = o.getHash();
        assertEquals(o, result.get("one"));
        assertEquals(o, result.get("two"));
    }

    @Test
    void withVoidPointer() {
        o.withVoidp(null);
        o.withVoidp(MemorySegment.NULL);
    }

    @Test
    void getObjects() {
        var objects = o.getObjects();
        assertIterableEquals(List.of(o), objects);
    }

    @Test
    void createObject() {
        var o2 = o.createObject();
        assertSame(o, o2);
    }

    @Test
    void buffer() {
        try (Arena arena = Arena.ofConfined()) {
            var bytes = arena.allocate(16);
            o.useBuffer(bytes);
        }
    }

    @Test
    void arrayZeroTerminated() {
        o.computeSum(new int[] {1, 2, 3});
    }

    @Test
    void arrayLength() {
        o.computeSumN(new int[] {1, 2, 3});
    }

    @Test
    void arrayZeroTerminatedAndLengthInt() {
        o.computeSumNz(new int[] {1, 2, 3});
    }

    @Test
    void arrayZeroTerminatedAndLengthString() {
        var argv = new Out<>(new String[] {"--num", "5", "--no-florp"});
        o.parseArgs(argv);
        var stringOut = new Out<String>();
        boolean result = o.stringOut(stringOut);
        assertFalse(result);
        assertNull(stringOut.get());
    }

    @Test
    void foreach() {
        o.foreach((_, _) -> {});
    }

    @Test
    void setArray() {
        o.setData(new byte[] {104, 105, 106, 107});
    }

    @Test
    void setArrayWithElementType() {
        o.setData2(new byte[] {104, 105, 106, 107});
    }

    @Test
    void setArrayWithOverriddenElementType() {
        o.setData3(new byte[] {104, 105, 106, 107});
    }

    @Test
    void allowNone() {
        assertNull(o.allowNone("foo"));
    }

    @Test
    void noTransfer() {
        assertNull(o.notrans());
    }

    @Test
    @SuppressWarnings("deprecation")
    void doNotUse() {
        assertNull(o.doNotUse());
    }

    @Test
    void watch() {
        o.watch((_, _) -> {});
    }

    @Test
    void hiddenSelf() {
        o.hiddenSelf();
    }

    @Test
    void annotationInit() {
        var argv = new Out<>(new String[] {"--num", "5", "--no-florp"});
        Regress.annotationInit(argv);
    }

    @Test
    void returnArray() {
        assertNull(Regress.annotationReturnArray());
    }

    @Test
    void returnStringZeroTerminated() {
        assertNull(Regress.annotationStringZeroTerminated());
    }

    @Test
    void outStringZeroTerminated() {
        var out = new Out<>(new String[] {"in", "out"});
        Regress.annotationStringZeroTerminatedOut(out);
        assertArrayEquals(new String[] {"in", "out"}, out.get());
    }

    @Test
    void versioned() {
        Regress.annotationVersioned();
    }

    @Test
    void stringArrayLength() {
        Regress.annotationStringArrayLength(new String[] {"foo", "bar"});
    }

    @Test
    void extraAnnos() {
        o.extraAnnos();
    }

    @Test @Disabled("Unsupported")
    void customDestroy() {
        // Regress.annotationCustomDestroy(a -> a);
        Regress.annotationCustomDestroyCleanup();
    }

    @Test
    void getSourceFile() {
        assertNull(Regress.annotationGetSourceFile());
    }

    @Test
    void setSourceFile() {
        Regress.annotationSetSourceFile("résumé.txt");
    }

    @Test
    void attributeFunc() {
        assertEquals(42, Regress.annotationAttributeFunc(o, "foo"));
    }

    @Test
    void invalidAnnotation() {
        Regress.annotationInvalidRegressAnnotation(42);
    }

    @Test
    void testParsingBug630862() {
        assertNull(Regress.annotationTestParsingBug630862());
    }

    @Test
    void testCommentBug631690() {
        Regress.annotationSpaceAfterCommentBug631690();
    }

    @Test
    void returnFilename() {
        assertEquals("a utf-8 filename", Regress.annotationReturnFilename());
    }

    @Test
    void transferFloating() {
        assertNull(Regress.annotationTransferFloating(o));
    }
}
