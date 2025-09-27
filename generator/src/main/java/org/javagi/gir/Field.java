/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 the Java-GI developers
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

package org.javagi.gir;

import static org.javagi.util.CollectionUtils.*;

import java.util.List;
import java.util.Map;

public final class Field extends GirElement implements TypedValue {

    public Field(Map<String, String> attributes, List<Node> children) {
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
        return TypedValue.super.allocatesMemory()
                || (callback() != null)
                || (anyType() instanceof Type type
                        && type.lookup() instanceof Callback);
    }

    /**
     * Get the memory size of this field, in bytes
     * @param longAsInt when true, long is 4 bytes, else long is 8 bytes
     */
    public int getSize(boolean longAsInt) {
        AnyType anyType = anyType();
        if (anyType instanceof Type t && t.isPointer())
            return 8;
        return anyType == null ? 8 // callback
                               : anyType.allocatedSize(longAsInt);
    }

    /**
     * Get the memory alignment of this field, in bytes
     * @param longAsInt when true, long is 4 bytes, else long is 8 bytes
     */
    public int getAlignment(boolean longAsInt) {
        AnyType anyType = anyType();
        if (anyType instanceof Type t)
            return t.isPointer() ? 8 : t.allocatedSize(longAsInt);
        if (anyType instanceof Array a)
            return a.fixedSize() < 0 ? 8 : a.elementSize(longAsInt);
        return 8; // callback
    }

    /**
     * Check whether this field should not be exposed
     */
    public boolean isDisguised() {
        // No getter/setter for a "disguised" record or private data
        if (anyType() instanceof Type type
                && type.lookup() instanceof Record r
                && (r.disguised() || r.skipJava()))
            return true;

        // Don't generate a getter/setter for the parent_class field (the first
        // field of a type struct).
        if (((FieldContainer) parent()).fields().getFirst() == this
                && parent().attr("glib:is-gtype-struct-for") != null)
            return true;

        // Don't generate a getter/setter for padding
        if ("padding".equals(name()))
            return true;

        // Don't generate a getter/setter for reserved space
        return name().contains("reserved");
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
