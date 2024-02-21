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

import io.github.jwharm.javagi.util.Platform;
import static io.github.jwharm.javagi.util.CollectionUtils.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract sealed class AbstractCallable extends GirElement implements Multiplatform, Callable
        permits Constructor, Function, Method, Signal, VirtualMethod {

    private int platforms;

    public AbstractCallable(Map<String, String> attributes, List<GirElement> children, int platforms) {
        super(attributes, children);
        this.platforms = platforms;
    }

    @Override
    public void setPlatforms(int platforms) {
        this.platforms = platforms;
    }

    @Override
    public int platforms() {
        return this.platforms;
    }

    @Override
    public String name() {
        return attrs().movedTo() != null ? attrs().movedTo()
                : attrs().shadows() != null ? attrs().shadows()
                : attrs().name();
    }

    @Override
    public boolean throws_() {
        return attrs().throws_();
    }

    @Override
    public boolean deprecated() {
        return attrs().deprecated();
    }

    // If true, this callable will not be generated
    public boolean skip() {
        // Explicit override: do not skip
        if (attrBool("java-gi-dont-skip", false))
            return false;

        // Do not generate virtual methods in interfaces
        if (this instanceof VirtualMethod vm && vm.parent() instanceof Interface)
            return true;

        // For virtual methods with an invoker method, only the invoker method
        // is generated
        if (this instanceof VirtualMethod vm && vm.invoker() != null)
            return true;

        // Shadowed by another method
        if (attrs().shadowedBy() != null)
            return true;

        // Replaced by another method
        if (attrs().movedTo() != null && attrs().movedTo().contains("."))
            return true;

        if (parameters() == null)
            return false;

        for (Parameter parameter : parameters().parameters()) {

            // va_list parameters are not supported
            if (parameter.anyType() instanceof Type type
                    && List.of("va_list", "va_list*").contains(type.cType()))
                return true;

            // Nested arrays are not supported yet
            if (parameter.anyType() instanceof Array array
                    && ((array.cType() != null && array.cType().endsWith("***"))
                        || array.anyType() instanceof Array
                        || (array.anyType() instanceof Type t && t.isActuallyAnArray())))
                return true;
        }

        return false;
    }

    public boolean doPlatformCheck() {
        if (platforms() == Platform.ALL) return false;
        if (this instanceof Constructor || this instanceof Function)
            return switch(parent()) {
                case RegisteredType rt -> rt.platforms();
                case Namespace ns -> ns.platforms();
                default -> throw new IllegalStateException("Illegal parent type");
            } != Platform.ALL;
        return false;
    }

    public CallableAttrs attrs() {
        return super.callableAttrs();
    }

    @Override
    public InfoElements infoElements() {
        return super.infoElements();
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
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AbstractCallable) obj;
        return Objects.equals(this.name(), that.name()) &&
                Objects.equals(this.throws_(), that.throws_()) &&
                Objects.equals(this.parameters(), that.parameters()) &&
                Objects.equals(this.returnValue(), that.returnValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name(), throws_(), parameters(), returnValue());
    }

    @Override
    public String toString() {
        return "%s %s %s %s".formatted(getClass().getSimpleName(), Platform.toString(platforms()), attributes(), children());
    }
}
