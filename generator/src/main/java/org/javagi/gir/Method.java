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
import java.util.Objects;

public final class Method extends Multiplatform implements Callable {

    public Method(Map<String, String> attributes,
                  List<Node> children,
                  int platforms) {
        super(attributes, children, platforms);
    }

    @Override
    public RegisteredType parent() {
        return (RegisteredType) super.parent();
    }

    public String setProperty() {
        return attr("set-property");
    }

    public String getProperty() {
        return attr("get-property");
    }

    public VirtualMethod invokerFor() {
        return parent().children().stream()
                .filter(VirtualMethod.class::isInstance)
                .map(VirtualMethod.class::cast)
                .filter(vm -> name().equals(vm.attr("invoker")))
                .filter(vm -> vm.equalTypeSignature(this))
                .findAny()
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Method other = (Method) o;
        return Objects.equals(callableAttrs().cIdentifier(),
                              other.callableAttrs().cIdentifier());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), callableAttrs().cIdentifier());
    }
}
