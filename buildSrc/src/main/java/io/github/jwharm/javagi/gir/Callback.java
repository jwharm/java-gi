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

import static io.github.jwharm.javagi.util.CollectionUtils.*;

import java.util.List;
import java.util.Map;

public final class Callback extends Multiplatform
        implements RegisteredType, Callable {

    public Callback(Map<String, String> attributes, List<Node> children, int platforms) {
        super(attributes, children, platforms);
    }

    @Override
    public String name() {
        return RegisteredType.super.name();
    }

    @Override
    public String glibTypeName() {
        return attr("c:type");
    }

    @Override
    public String getTypeFunc() {
        return null; // Callback has no glib:get-type
    }

    @Override
    public Callback mergeWith(RegisteredType rt) {
        // No need to merge
        return this;
    }

    @Override
    public boolean throws_() {
        return attrBool("throws", false);
    }

    @Override
    public Parameters parameters() {
        return findAny(children(), Parameters.class);
    }

    @Override
    public ReturnValue returnValue() {
        return findAny(children(), ReturnValue.class);
    }

    @Override
    public boolean deprecated() {
        return infoAttrs().deprecated();
    }
}
