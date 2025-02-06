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

import java.util.List;

import static io.github.jwharm.javagi.util.CollectionUtils.findAny;

public sealed interface Callable
        extends Node
        permits Callback, Constructor, Function, Method, Signal, VirtualMethod {

    int platforms();
    InfoElements infoElements();
    CallableAttrs callableAttrs();

    default String name() {
        return callableAttrs().movedTo() != null ? callableAttrs().movedTo()
                : callableAttrs().shadows() != null ? callableAttrs().shadows()
                : callableAttrs().name();
    }

    default boolean throws_() {
        return this.callableAttrs().throws_();
    }

    default boolean deprecated() {
        return this.callableAttrs().deprecated();
    }

    // If true, this callable will not be generated
    default boolean skip() {
        // Explicit override: do not skip
        if (attrBool("java-gi-dont-skip", false))
            return false;

        // Do not generate unnamed, parameter-less constructors
        if (this instanceof Constructor ctr
                && "new".equals(ctr.name())
                && (ctr.parameters() == null || ctr.parameters().parameters().isEmpty()))
            return true;

        // Do not generate virtual methods in interfaces
        if (this instanceof VirtualMethod vm
                && vm.parent() instanceof Interface)
            return true;

        // For virtual methods with an invoker method, only the invoker method
        // is generated
        if (this instanceof VirtualMethod
                vm && vm.invoker() != null)
            return true;

        // Shadowed by another method
        if (this.callableAttrs().shadowedBy() != null)
            return true;

        // Replaced by another method
        if (this.callableAttrs().movedTo() != null
                && this.callableAttrs().movedTo().contains("."))
            return true;

        if (parameters() == null)
            return false;

        for (Parameter parameter : parameters().parameters()) {

            // va_list parameters are not supported
            if (parameter.anyType() instanceof Type type
                    && List.of("va_list", "va_list*").contains(type.cType()))
                return true;
        }

        return false;
    }

    /**
     * Return true when there is one or more bitfield parameters.
     * An alias for a bitfield is counted as a bitfield as well.
     * Out-parameters are not counted as bitfield parameters.
     */
    default boolean hasBitfieldParameters() {
        Parameters params = parameters();
        if (params == null)
            return false;
        for (Parameter p : params.parameters()) {
            if (p.isOutParameter())
                continue;
            if (p.anyType() instanceof Type t && t.isPointer())
                continue;
            if (p.isBitfield())
                return true;
        }
        return false;
    }

    default Parameters parameters() {
        return findAny(children(), Parameters.class);
    }

    default ReturnValue returnValue() {
        return findAny(children(), ReturnValue.class);
    }

    default boolean allocatesMemory() {
        if (throws_() || returnValue().allocatesMemory())
            return true;
        if (parameters() != null)
            return parameters().parameters().stream()
                    .anyMatch(Parameter::allocatesMemory);
        return false;
    }
}
