/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 the Java-GI developers
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

package io.github.jwharm.javagi.gir;

import static io.github.jwharm.javagi.util.CollectionUtils.*;
import static io.github.jwharm.javagi.util.Conversions.toJavaBaseType;

import java.util.List;
import java.util.Map;

public final class Field extends TypedValue {

    private final int index;

    public Field(Map<String, String> attributes, List<GirElement> children, int index) {
        super(attributes, children);
        this.index = index;
        if ((callback() == null) == (anyType() == null))
            throw new IllegalArgumentException("Either Callback | AnyType must be set");
    }

    @Override
    public RegisteredType parent() {
        return (RegisteredType) super.parent();
    }

    public int index() {
        return index;
    }

    @Override
    public boolean allocatesMemory() {
        return super.allocatesMemory()
                || (callback() != null)
                || (anyType() instanceof Type type && type.get() instanceof Callback);
    }

    /**
     * Get the native type of this field. For example "int", "char".
     * An array with fixed size is returned as "ARRAY", a pointer is returned as "java.lang.foreign.MemorySegment".
     */
    public String getMemoryType() {
        return switch (anyType()) {
            case null -> "java.lang.foreign.MemorySegment"; // callback
            case Type type -> getMemoryType(type);
            case Array array -> array.fixedSize() > 0 ? "ARRAY" : "java.lang.foreign.MemorySegment";
        };
    }

    private String getMemoryType(Type type) {
        RegisteredType target = type.get();

        if (type.isPointer() && (type.isPrimitive() || target instanceof Bitfield || target instanceof Enumeration))
            return "java.lang.foreign.MemorySegment";

        if (type.isBoolean() || target instanceof Bitfield || target instanceof Enumeration)
            return "int";

        if (type.isPrimitive())
            return toJavaBaseType(type.name());

        if (target instanceof Alias alias && alias.type().isPrimitive())
            return toJavaBaseType(alias.type().name());

        return "java.lang.foreign.MemorySegment";
    }

    /**
     * Get the memory size of this field, in bytes
     */
    public int getSize() {
        return getSize(getMemoryType());
    }

    private int getSize(String memoryType) {
        return switch(memoryType) {
            case "byte", "char" -> 1;
            case "short" -> 2;
            case "boolean", "int", "float" -> 4;
            case "long", "double", "java.lang.foreign.MemorySegment" -> 8;
            case "ARRAY" -> {
                Array array = (Array) anyType();
                int size = switch (array.anyType()) {
                    case Array _ -> 8;
                    case Type type -> getSize(getMemoryType(type));
                };
                yield array.fixedSize() * size;
            }
            default -> throw new IllegalArgumentException("Unknown memory type " + memoryType);
        };
    }

    /**
     * Check whether this field should not be exposed
     */
    public boolean isDisguised() {
        // Don't generate a getter/setter for a "disguised" record or private data
        if (anyType() instanceof  Type type
                && type.get() instanceof Record r
                && (r.disguised() || r.name().endsWith("Private")))
            return true;
        // Don't generate a getter/setter for padding
        if ("padding".equals(name()))
            return true;
        // Don't generate a getter/setter for reserved space
        return name().contains("reserved");
    }

    public InfoAttrs attrs() {
        return infoAttrs();
    }

    public boolean writable() {
        return attrBool("writable", false);
    }

    public boolean readable() {
        return attrBool("readable", true);
    }

    public boolean private_() {
        return attrBool("private", false);
    }

    public int bits() {
        return attrInt("bits");
    }

    public Callback callback() {
        return findAny(children(), Callback.class);
    }
}
