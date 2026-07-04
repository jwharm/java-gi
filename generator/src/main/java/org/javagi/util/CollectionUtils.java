/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2026 the Java-GI developers
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

package org.javagi.util;

import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

/**
 * Utility functions for working with collections.
 */
public class CollectionUtils {

    /**
     * Return the union of two lists. The ordering is retained.
     */
    public static <T> List<T> union(List<T> list1, List<T> list2) {
        return Stream.concat(list1.stream(), list2.stream())
                .distinct()
                .toList();
    }

    /**
     * Filter the list for members of the requested type. Returns an
     * unmodifiable list.
     */
    @SuppressWarnings("unchecked") // cast is checked by cls::isInstance
    public static <A, B extends A> List<B> filter(List<A> list, Class<B> cls) {
        return (List<B>) list.stream()
                .filter(cls::isInstance)
                .toList();
    }

    /**
     * Search the list for a member of the requested type. Returns {@code null}
     * when not found.
     */
    @SuppressWarnings("unchecked") // cast is checked by cls::isInstance
    public static <A, B extends A> @Nullable B findAny(List<A> list, Class<B> cls) {
        return (B) list.stream()
                .filter(cls::isInstance)
                .findAny()
                .orElse(null);
    }

    /**
     * Return a view of the list with all elements except the first. When the
     * size of the list is 0 or 1, an empty list is returned.
     */
    public static <A> List<A> tail(List<A> list) {
        if (list == null || list.isEmpty())
            return emptyList();

        return list.subList(1, list.size());
    }

    /**
     * Return the union of two maps by creating a new map using
     * {@code new HashMap(map1).putAll(map2)}
     */
    public static <A, B> Map<A, B> union(Map<A, B> map1, Map<A, B> map2) {
        Map<A, B> map3 = new HashMap<>(map1);
        map3.putAll(map2);
        return map3;
    }

    /**
     * Similar to {@link List#of}, but ignores {@code null} arguments.
     */
    @SafeVarargs
    public static <T> List<T> listOfNonNull(T... elements) {
        if (elements == null)
            return emptyList();

        return Stream.of(elements)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
