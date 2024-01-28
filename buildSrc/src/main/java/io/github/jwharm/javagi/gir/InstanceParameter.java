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
import java.util.Map;

public final class InstanceParameter extends TypedValue {

    public InstanceParameter(Map<String, String> attributes, List<GirElement> children) {
        super(attributes, children);
        if (anyType() instanceof Array) {
            throw new UnsupportedOperationException("InstanceParameter cannot be an array");
        }
    }

    @Override
    public Parameters parent() {
        return (Parameters) super.parent();
    }

    @Override
    public boolean allocatesMemory() {
        return false;
    }

    public boolean nullable() {
        return attrBool("nullable", false);
    }

    @Deprecated
    public boolean allowNone() {
        return attrBool("allow-none", false);
    }

    public Direction direction() {
        return Direction.from(attr("direction"));
    }

    public boolean callerAllocates() {
        return attrBool("caller-allocates", true);
    }

    public TransferOwnership transferOwnership() {
        return TransferOwnership.from(attr("transfer-ownership"));
    }
}
