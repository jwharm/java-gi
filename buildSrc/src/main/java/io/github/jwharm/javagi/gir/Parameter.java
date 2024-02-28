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

public final class Parameter extends GirElement implements TypedValue {

    public Parameter(Map<String, String> attributes, List<Node> children) {
        super(attributes, children);
    }

    @Override
    public Parameters parent() {
        return (Parameters) super.parent();
    }

    public boolean isOutParameter() {
        if (anyType() instanceof Array a && a.unknownSize())
            return false;

        return (direction() == Direction.OUT
                    || direction() == Direction.INOUT)
                && (anyType() instanceof Array
                    || (anyType() instanceof Type type
                        && (type.isPointer()
                            || (type.cType()) != null
                                && type.cType().endsWith("gsize")))
                        && (!type.isProxy())
                        && (!(type.get() instanceof Alias a
                                && a.type().isPrimitive())));
    }

    public boolean isUserDataParameter() {
        // Callback parameters: the user_data parameter has attribute "closure" set
        if (parent().parent() instanceof Callback
                || parent().parent() instanceof Signal)
            return (attr("closure") != null);

        // Method parameters that pass a user_data pointer to a closure
        if (anyType() instanceof Type t
                && List.of("gpointer", "gconstpointer").contains(t.cType())) {
            return parent().parameters().stream().anyMatch(p ->
                    p.anyType() instanceof Type type
                            && type.get() instanceof Callback
                            && p.closure() == this
            );
        }
        return false;
    }

    public boolean isDestroyNotifyParameter() {
        return (anyType() instanceof Type type)
                && "GDestroyNotify".equals(type.cType());
    }

    public boolean isErrorParameter() {
        return (anyType() instanceof Type type)
                && "GError**".equals(type.cType());
    }

    public boolean isArrayLengthParameter() {
        // Check return value
        if (parent().parent() instanceof Callable callable
                && callable.returnValue().anyType() instanceof Array array
                && array.length() == this)
            return true;

        // Check other parameters
        return parent().parameters().stream().anyMatch(p ->
                p.anyType() instanceof Array a && a.length() == this
        );
    }

    @Override
    public boolean allocatesMemory() {
        if (TypedValue.super.allocatesMemory() || isOutParameter())
            return true;

        Type type = (Type) anyType();
        RegisteredType target = type.get();

        if (target instanceof Callback)
            return true;

        return type.isPointer()
                && target instanceof Alias a && a.type().isPrimitive();
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

    public boolean introspectable() {
        return attrBool("introspectable", true);
    }

    public Parameter closure() {
        return parent().getAtIndex(attrInt("closure"));
    }

    public Parameter destroy() {
        return parent().getAtIndex(attrInt("destroy"));
    }

    public Scope scope() {
        Scope scope = Scope.from(attr("scope"));
        return (scope == Scope.NOTIFIED && destroy() == null)
                ? Scope.FOREVER
                : scope;
    }

    public Direction direction() {
        return Direction.from(attr("direction"));
    }

    public boolean callerAllocates() {
        return attrBool("caller-allocates", true);
    }

    public boolean skip() {
        return attrBool("skip", false);
    }

    public TransferOwnership transferOwnership() {
        return TransferOwnership.from(attr("transfer-ownership"));
    }

    public boolean varargs() {
        return findAny(children(), Varargs.class) != null;
    }
}
