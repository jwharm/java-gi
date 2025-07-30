/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 Jan-Willem Harmannij
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

package org.javagi.gobject;

import org.gnome.glib.Type;
import org.gnome.gobject.GObjects;
import org.javagi.base.Proxy;
import org.javagi.gobject.types.Types;
import org.javagi.interop.Interop;

import java.lang.foreign.MemorySegment;
import java.util.function.Consumer;

/**
 * Utility functions for boxed types
 */
public class BoxedUtil {

    /**
     * Checks if {@code type} is a boxed type.
     *
     * @param  type A {@link Type} value
     * @return {@code true} if {@code type} is a boxed type
     */
    public static boolean isBoxed(Type type) {
        return GObjects.typeFundamental(type).equals(Types.BOXED);
    }

    /**
     * Make a copy of {@code struct}. If {@code type} is a boxed type, use
     * {@code g_boxed_copy}, or else, {@code malloc} a new struct and copy
     * {@code size} bytes from {@code struct} to it.
     *
     * @param type   the type, possibly a boxed type
     * @param struct the struct to be copied
     * @param size   the size of the struct (only used when {@code type} is not
     *               a boxed type)
     * @return the newly created copy of {@code struct}. The caller has
     *         ownership and is responsible for freeing it.
     */
    public static <T extends Proxy> MemorySegment copy(Type type, T struct, long size) {
        if (struct == null || struct.handle() == null)
            return null;

        if (struct.handle().equals(MemorySegment.NULL))
            return MemorySegment.NULL;

        if (type != null && isBoxed(type))
            return GObjects.boxedCopy(type, struct.handle());

        MemorySegment copy = Interop.mallocAllocator().allocate(size);
        Interop.copy(struct.handle(), copy, size);
        return copy;
    }

    /**
     * Free {@code struct}. If {@code type} is a boxed type, use
     * {@code g_boxed_free}, or else, apply {@code freeFunc} on {@code struct}.
     *
     * @param type     the type, possibly a boxed type
     * @param struct   the struct to be freed
     * @param freeFunc a function that will free {@code struct} (only used when
     *                 {@code type} is not a boxed type)
     */
    public static <T extends Proxy> void free(Type type, T struct, Consumer<T> freeFunc) {
        if (struct == null || struct.handle() == null || struct.handle().equals(MemorySegment.NULL))
            return;

        if (type != null && isBoxed(type))
            GObjects.boxedFree(type, struct.handle());
        else if (freeFunc != null)
            freeFunc.accept(struct);
    }
}
