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

import static org.javagi.util.CollectionUtils.*;

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

    /*
     * Check if the parameter has direction "out" or "inout", and also check:
     * - It's not an array of unknown size
     * - It's not a raw pointer
     * - It's not a proxy type
     * - It's not an alias wrapping a primitive value
     * This is used for all parameters that will be wrapped in an Out<> class in Java.
     */
    public boolean isOutParameter() {
        if (anyType() instanceof Array a && a.unknownSize())
            return false;

        if (direction() == null || direction() == Direction.IN)
            return false;

        if (anyType() instanceof Array)
            return true;

        return (anyType() instanceof Type type
                && (type.isPointer() || (type.cType()) != null && type.cType().endsWith("gsize")))
                && (!type.isProxy())
                && (!(type.lookup() instanceof Alias a && a.isValueWrapper()));
    }

    public boolean isUserDataParameter() {
        // Callback params: the user_data parameter has attribute "closure" set
        if (parent().parent() instanceof Callback
                || parent().parent() instanceof Signal)
            return (attr("closure") != null);

        // Method parameters that pass a user_data pointer to a closure
        if (anyType() instanceof Type t) {
            return parent().parameters().stream().anyMatch(p ->
                    p.anyType() instanceof Type type
                            && type.lookup() instanceof Callback
                            && p.closure() == this);
        }
        return false;
    }

    public boolean isUserDataParameterForDestroyNotify() {
        return parent().parameters().stream().anyMatch(p ->
                p.anyType() instanceof Type type
                        && type.lookup() instanceof Callback
                        && p.scope() == Scope.NOTIFIED
                        && p.closure() == this
                        && p.destroy() != null);
    }

    public Parameter getRelatedCallbackParameter() {
        return parent().parameters().stream().filter(p ->
                p.closure() == this).findAny().orElseThrow();
    }

    public boolean isDestroyNotifyParameter() {
        return (anyType() instanceof Type type)
                && ("GDestroyNotify".equals(type.cType())
                    || "GDestroyNotify*".equals(type.cType()));
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
        if (varargs())
            return false;

        if (TypedValue.super.allocatesMemory() || isOutParameter())
            return true;

        Type type = (Type) anyType();
        RegisteredType target = type.lookup();

        if (target instanceof Callback && Scope.CALL.equals(scope()))
            return true;

        return type.isPointer()
                && target instanceof Alias a && a.isValueWrapper();
    }

    public boolean isLastParameter() {
        return this == parent().parameters().getLast();
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

    @Override
    public TransferOwnership transferOwnership() {
        return TransferOwnership.from(attr("transfer-ownership"));
    }

    public boolean varargs() {
        return findAny(children(), Varargs.class) != null;
    }
}
