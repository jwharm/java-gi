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

package io.github.jwharm.javagi.gir;

import io.github.jwharm.javagi.util.PartialStatement;

import static io.github.jwharm.javagi.util.CollectionUtils.*;

import java.util.List;

public sealed interface FlaggedType
        extends RegisteredType
        permits Bitfield, Enumeration {

    @Override
    default PartialStatement constructorName() {
        return PartialStatement.of("$" + typeTag() + ":T::of",
                typeTag(), typeName());
    }

    @Override
    default PartialStatement destructorName() {
        return PartialStatement.of("(_ -> {})");
    }

    default List<Member> members() {
        return filter(children(), Member.class);
    }

    default List<Function> functions() {
        return filter(children(), Function.class);
    }
}
