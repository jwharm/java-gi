/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 Jan-Willem Harmannij
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
package org.gnome.glib;

import io.github.jwharm.javagi.base.ProxyInstance;
import io.github.jwharm.javagi.interop.Interop;
import io.github.jwharm.javagi.interop.MemoryCleaner;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import static org.gnome.glib.GLib.malloc;

/**
 * This class is used for internal purposes by Java-GI. Java-GI
 * automatically marshals {@code GByteArray} instances from and to java
 * {@code byte[]} primitives.
 */
public class ByteArray extends ProxyInstance {
    static {
        GLib.javagi$ensureInitialized();
    }

    /**
     * Create a ByteArray proxy instance for the provided memory address.
     *
     * @param address the memory address of the native object
     */
    public ByteArray(MemorySegment address) {
        super(Interop.reinterpret(address, getMemoryLayout().byteSize()));
    }

    /**
     * The memory layout of the native struct.
     * @return the memory layout
     */
    public static MemoryLayout getMemoryLayout() {
        return MemoryLayout.structLayout(
                ValueLayout.ADDRESS.withName("data"),
                MemoryLayout.paddingLayout(3),
                ValueLayout.JAVA_INT.withName("len")
        ).withName("GByteArray");
    }

    /**
     * Read the value of the field {@code len}.
     *
     * @return The value of the field {@code len}
     */
    public int readLen() {
        return (int) MethodHandles.vh_len.get(handle(), 0);
    }

    /**
     * Creates a new {@code GByteArray} with a reference count of 1.
     */
    public ByteArray() {
        this(constructNew());
        MemoryCleaner.takeOwnership(this);
        MemoryCleaner.setFreeFunc(this, "g_byte_array_unref");
    }

    private static MemorySegment constructNew() {
        MemorySegment _result;
        try {
            _result = (MemorySegment) MethodHandles.g_byte_array_new.invokeExact();
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        return _result;
    }

    /**
     * Creates a byte array containing the {@code data}.
     *
     * @param data byte data for the array
     * @return a new {@code GByteArray}
     */
    public static ByteArray take(byte[] data) {
        var instance = takeUnowned(data);
        if (instance != null) {
            MemoryCleaner.takeOwnership(instance);
            MemoryCleaner.setFreeFunc(instance, "g_byte_array_unref");
        }
        return instance;
    }

    /**
     * Creates a byte array containing the {@code data}. The allocated native
     * memory is not freed automatically by Java-GI.
     *
     * @param data byte data for the array
     * @return a new {@code GByteArray}
     */
    public static ByteArray takeUnowned(byte[] data) {
        if (data == null)
            return new ByteArray();

        MemorySegment result;
        try {
            MemorySegment segment = malloc(data.length);
            segment.asByteBuffer().put(data, 0, data.length);
            result = (MemorySegment) MethodHandles.g_byte_array_new_take.invokeExact(segment, data.length);
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        return MemorySegment.NULL.equals(result) ? null : new ByteArray(result);
    }

    private static final class MethodHandles {
        static final VarHandle vh_len = getMemoryLayout().varHandle(
                MemoryLayout.PathElement.groupElement("len"));

        static final MethodHandle g_byte_array_new = Interop.downcallHandle("g_byte_array_new",
                FunctionDescriptor.of(ValueLayout.ADDRESS), false);

        static final MethodHandle g_byte_array_new_take = Interop.downcallHandle(
                "g_byte_array_new_take", FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), false);
    }
}
