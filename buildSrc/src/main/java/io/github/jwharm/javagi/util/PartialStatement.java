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

import com.squareup.javapoet.TypeName;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to construct JavaPoet code blocks with named arguments from multiple
 * smaller parts.
 */
public final class PartialStatement {

    private final StringBuilder format = new StringBuilder();
    private final Map<String, Object> arguments = new HashMap<>();

    public PartialStatement() {
    }

    public static PartialStatement of(String format, Object... args) {
        return new PartialStatement().add(format, args);
    }

    public PartialStatement add(String format, Object... args) {
        if (format != null)
            this.format.append(format);

        if ((args.length & 1) == 1)
            throw new IllegalArgumentException("Arguments must be a list of key-value pairs");

        for (int i = 0; i < args.length; i += 2) {
            String key = args[i].toString();
            Object value = args[i + 1];

            // Create TypeNames for all Class<?> parameters, so they will be
            // regarded as equal when comparing them (below)
            if (value instanceof Class<?> c)
                value = TypeName.get(c);

            // When an existing tag is reassigned to a different typename, it
            // is almost certainly a bug
            Object previous = arguments.put(args[i].toString(), value);
            if (previous != null && (!previous.equals(value)))
                throw new IllegalArgumentException("Argument %s overrules %s with %s"
                        .formatted(key, previous, value));
        }

        return this;
    }

    public PartialStatement add(PartialStatement other) {
        if (other == null)
            return this;

        add(other.format());
        other.arguments().forEach((key, value) -> add(null, key, value));
        return this;
    }

    public String format() {
        return format.toString();
    }

    public Map<String, Object> arguments() {
        return arguments;
    }
}
