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

package org.javagi.metadata;

import java.util.List;
import java.util.Map;

/**
 * Represents a metadata rule: a pattern, selector, zero or more arguments, and
 * zero or more nested (relative) rules.
 *
 * @param glob     a glob pattern to match against Gir node names
 * @param selector an optional selector of the Gir element type
 * @param args     argument names and values
 * @param children nested (relative) rules
 */
public record Rule(String glob, String selector, Map<String, String> args,
                   List<Rule> children) {
}
