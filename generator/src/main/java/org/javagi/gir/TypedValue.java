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

import com.squareup.javapoet.TypeName;

import static org.javagi.util.CollectionUtils.*;

public sealed interface TypedValue
        extends Node
        permits Constant, Field, InstanceParameter, Parameter, Property,
                ReturnValue {

    InfoElements infoElements();

    default String name() {
        return attr("name");
    }

    default AnyType anyType() {
        return findAny(children(), AnyType.class);
    }

    default boolean allocatesMemory() {
        return switch(anyType()) {
            case null -> true; // callback
            case Array _ -> true;
            case Type type -> type.isActuallyAnArray()
                    || TypeName.get(String.class).equals(type.typeName());
        };
    }

    default TransferOwnership transferOwnership() {
        return TransferOwnership.NONE;
    }

    default boolean isBitfield() {
        if (anyType() instanceof Type type && (!type.isPrimitive())) {
            RegisteredType target = type.lookup();
            if (target instanceof Alias alias)
                target = alias.lookup();
            return target instanceof Bitfield;
        }
        return false;
    }
}
