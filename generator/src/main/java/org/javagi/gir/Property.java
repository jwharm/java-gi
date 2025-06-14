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

public final class Property extends Multiplatform implements TypedValue {

    public Property(Map<String, String> attributes,
                    List<Node> children,
                    int platforms) {
        super(attributes, children, platforms);
    }

    @Override
    public RegisteredType parent() {
        return (RegisteredType) super.parent();
    }

    public boolean writable() {
        return attrBool("writable", false);
    }

    public boolean readable() {
        return attrBool("readable", true);
    }

    public boolean construct() {
        return attrBool("construct", false);
    }

    public boolean constructOnly() {
        return attrBool("construct-only", false);
    }

    public String setter() {
        return attr("setter");
    }

    public String getter() {
        return attr("getter");
    }

    public String defaultValue() {
        return attr("default-value");
    }

    @Override
    public TransferOwnership transferOwnership() {
        return TransferOwnership.from(attr("transfer-ownership"));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || obj.getClass() != this.getClass())
            return false;

        var that = (Property) obj;
        return Objects.equals(this.name(), that.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name());
    }
}
