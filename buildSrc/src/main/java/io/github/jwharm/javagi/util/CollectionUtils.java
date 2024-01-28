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

package io.github.jwharm.javagi.util;

import io.github.jwharm.javagi.gir.Multiplatform;

import java.util.*;

/**
 * Small utility functions for working with collections
 */
public class CollectionUtils {

    /**
     * Return the union of two lists.
     * Updates the {@code platforms} property of {@link Multiplatform} instances.
     * The ordering is retained.
     */
    public static <T> List<T> union(List<T> list1, List<T> list2) {
        List<T> result = new ArrayList<>(list1);
        for (T element : list2) {
            int idx = result.indexOf(element);
            if (idx == -1)
                result.add(element);
            else if (element instanceof Multiplatform mp) {
                var existing = (Multiplatform) result.get(idx);
                existing.setPlatforms(existing.platforms() | mp.platforms());
            }
        }
        return result;
    }

    /**
     * Filter the list for members of the requested type. Returns an unmodifiable list.
     */
    public static <A, B extends A> List<B> filter(List<A> list, Class<B> cls) {
        return list.stream().filter(cls::isInstance).map(cls::cast).toList();
    }

    /**
     * Search the list for a member of the requested type. Returns {@code null} when not found.
     */
    public static <A, B extends A> B findAny(List<A> list, Class<B> cls) {
        return list.stream().filter(cls::isInstance).map(cls::cast).findAny().orElse(null);
    }
}
