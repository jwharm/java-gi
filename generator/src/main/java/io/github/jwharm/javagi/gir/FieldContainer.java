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

import java.util.List;

public sealed interface FieldContainer
        extends RegisteredType
        permits Class, Interface, Boxed, Record, Union {

    List<Field> fields();

    default boolean opaque() {
        return false;
    }

    default Field getAtIndex(int index) {
        return index == -1 ? null : fields().get(index);
    }

    /**
     * If one of the fields directly refers to an opaque struct (recursively),
     * we cannot generate the memory layout or allocate memory for this type.
     *
     * @return whether on of the fields refers (recursively) to an opaque
     *         struct
     */
    default boolean hasOpaqueStructFields() {
        for (Field field : fields())
            if (field.anyType() instanceof Type type
                    && !type.isPointer()
                    && type.lookup() instanceof FieldContainer fc
                    && (fc.opaque() || fc.hasOpaqueStructFields()))
                return true;
        return false;
    }
}
