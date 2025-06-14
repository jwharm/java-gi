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

import java.util.List;
import java.util.Map;

public final class ReturnValue extends GirElement implements TypedValue {

    public ReturnValue(Map<String, String> attributes, List<Node> children) {
        super(attributes, children);
    }

    @Override
    public String name() {
        throw new UnsupportedOperationException("ReturnValue does not have a name");
    }

    @Override
    public boolean allocatesMemory() {
        if (parent() instanceof Callback || parent() instanceof Signal
                && TypedValue.super.allocatesMemory())
            return true;

        return switch(anyType()) {
            case Array _ -> true;
            case Type type -> List.of(Scope.CALL, Scope.ASYNC).contains(scope())
                    && type.lookup() instanceof Callback;
        };
    }

    public String overrideValue() {
        return attr("java-gi-override-value");
    }

    public boolean introspectable() {
        return attrBool("introspectable", true);
    }

    public boolean nullable() {
        return "1".equals(attr("nullable"))
                || "1".equals(attr("allow-none"))
                || "1".equals(attr("optional"));
    }

    public boolean notNull() {
        return "0".equals(attr("nullable"))
                || "0".equals(attr("allow-none"))
                || "0".equals(attr("optional"));
    }

    public Parameter closure() {
        Callable parent = (Callable) parent();
        return parent.parameters().getAtIndex(attrInt("closure"));
    }

    public Scope scope() {
        return Scope.from(attr("scope"));
    }

    public Parameter destroy() {
        Callable parent = (Callable) parent();
        return parent.parameters().getAtIndex(attrInt("destroy"));
    }

    public boolean skip() {
        return attrBool("skip", false);
    }

    @Deprecated
    public boolean allowNone() {
        return attrBool("allow-none", false);
    }

    @Override
    public TransferOwnership transferOwnership() {
        return TransferOwnership.from(attr("transfer-ownership"));
    }
}
