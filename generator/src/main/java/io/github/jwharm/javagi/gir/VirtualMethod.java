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

public final class VirtualMethod extends Multiplatform implements Callable {

    public VirtualMethod(Map<String, String> attributes,
                         List<Node> children,
                         int platforms) {
        super(attributes, children, platforms);
    }

    public Method invoker() {
        String invoker = attr("invoker");
        if (invoker == null)
            return null;

        return parent().children().stream()
                .filter(Method.class::isInstance)
                .map(Method.class::cast)
                .filter(m -> invoker.equals(m.name()))
                .filter(this::equalTypeSignature)
                .findAny()
                .orElse(null);
    }

    public String overrideVisibility() {
        return attr("java-gi-override-visibility");
    }

    /*
     * Check if the invoker method has the same type signature as this virtual
     * method.
     *
     * We deliberately don't compare the return types, because two Java methods
     * with the same name and parameter types but different return types will
     * not compile.
     */
    boolean equalTypeSignature(Method m) {
        // Compare instance parameter type
        if (different(parameters().instanceParameter(),
                      m.parameters().instanceParameter()))
            return false;

        // Compare exceptions
        if (throws_() != m.throws_())
            return false;

        List<Parameter> params1 = parameters().parameters();
        List<Parameter> params2 = m.parameters().parameters();

        // Compare number of parameters
        if (params1.size() != params2.size())
            return false;

        // Compare parameter types
        for (int i = 0; i < params1.size(); i++)
            if (different(params1.get(i), params2.get(i)))
                return false;

        return true;
    }

    // Return true when these types would be different in Java
    private boolean different(TypedValue a, TypedValue b) {
        return !a.anyType().typeName().equals(b.anyType().typeName());
    }
}
