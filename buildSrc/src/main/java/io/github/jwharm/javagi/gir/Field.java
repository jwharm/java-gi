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

import com.squareup.javapoet.TypeName;

import static io.github.jwharm.javagi.util.CollectionUtils.*;

import java.util.List;
import java.util.Map;

public final class Field extends TypedValue {

    public Field(Map<String, String> attributes, List<GirElement> children) {
        super(attributes, children);
        if ((callback() == null) == (anyType() == null))
            throw new IllegalArgumentException("Either Callback | AnyType must be set");
    }

    @Override
    public RegisteredType parent() {
        return (RegisteredType) super.parent();
    }

    @Override
    public boolean allocatesMemory() {
        return super.allocatesMemory()
                || (callback() != null)
                || (anyType() instanceof Type type && type.get() instanceof Callback);
    }

    /**
     * Get the memory size of this field, in bytes
     */
    public int getSize() {
        return switch(anyType()) {
            case null -> 8; // callback
            case Array array -> getSize(array);
            case Type type -> getSize(type);
        };
    }

    private int getSize(Array array) {
        int fixedSize = array.fixedSize();
        if (fixedSize == -1)
            return 8;
        return fixedSize * switch(array.anyType()) {
            case Array nested -> getSize(nested);
            case Type type -> getSize(type);
        };
    }

    private int getSize(Type type) {
        var typeName = type.typeName();
        if (List.of(TypeName.BYTE, TypeName.CHAR).contains(typeName))
            return 1;
        if (TypeName.SHORT.equals(typeName))
            return 2;
        if (List.of(TypeName.BOOLEAN, TypeName.INT, TypeName.FLOAT).contains(typeName))
            return 4;
        if (type.get() instanceof FlaggedType)
            return 4;
        else
            return 8;
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
