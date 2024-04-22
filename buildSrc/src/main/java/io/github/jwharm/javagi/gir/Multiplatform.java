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

import java.util.List;
import java.util.Map;

public abstract sealed class Multiplatform
        extends GirElement
        permits Namespace, Alias, Boxed, Callback, Class, Bitfield,
                Enumeration, Interface, Record, Union, Constructor, Function,
                Method, Signal, VirtualMethod, Constant, Property {

    private int platforms;

    public Multiplatform(Map<String, String> attributes, List<Node> children, int platforms) {
        super(attributes, children);
        this.platforms = platforms;
    }

    public final void setPlatforms(int platforms) {
        this.platforms = platforms;
    }

    public final int platforms() {
        return this.platforms;
    }

    public boolean doPlatformCheck() {
        if (platforms() == Platform.ALL)
            return false;

        if (this instanceof RegisteredType)
            return true;

        if (this instanceof Constructor || this instanceof Function)
            return switch(parent()) {
                case Namespace ns -> ns.platforms();
                case RegisteredType rt -> rt.platforms();
                default -> throw new IllegalStateException("Illegal parent type");
            } != Platform.ALL;
        return false;
    }

    @Override
    public String toString() {
        return "%s %s %s %s".formatted(
                getClass().getSimpleName(),
                Platform.toString(platforms()),
                attributes(),
                children()
        );
    }
}
