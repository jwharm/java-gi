/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 the Jan-Willem Harmannij
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

package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.gir.GirElement;
import io.github.jwharm.javagi.gir.Method;
import io.github.jwharm.javagi.gir.VirtualMethod;
import io.github.jwharm.javagi.util.Patch;

public class PangoPatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        if (!"Pango".equals(namespace))
            return element;

        /*
         * Unsure how to interpret this return type:
         *
         * <array c:type="PangoLanguage**">
         *   <type name="Language"/>
         * </array>
         *
         * Removing the method from the Java bindings for now.
         */
        if (element instanceof Class c
                && "Font".equals(c.name())) {
            c = remove(c, Method.class, "name", "get_languages");

            /*
             * Font::getFeatures has different parameter attributes between
             * platforms. Remove the Java binding for now.
             */
            c = remove(c, Method.class, "name", "get_features");
            return remove(c, VirtualMethod.class, "name", "get_features");
        }

        /*
         * Java-GI automatically calls ref() but this one is deprecated.
         * We can safely remove it from the Java bindings.
         */
        if (element instanceof Class c && "Coverage".equals(c.name()))
            return remove(c, Method.class, "name", "ref");

        return element;
    }
}
