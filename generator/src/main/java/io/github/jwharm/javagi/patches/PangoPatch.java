/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 the Jan-Willem Harmannij
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
import io.github.jwharm.javagi.gir.Function;
import io.github.jwharm.javagi.gir.Method;
import io.github.jwharm.javagi.gir.Namespace;
import io.github.jwharm.javagi.gir.VirtualMethod;
import io.github.jwharm.javagi.util.Patch;

import java.util.List;

public class PangoPatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        if (!"Pango".equals(namespace))
            return element;

        if (element instanceof Namespace ns) {

            /*
             * pango_log2vis_get_embedding_levels has missing annotations.
             */
            return remove(ns, Function.class,
                    "c:identifier", "pango_log2vis_get_embedding_levels");
        }

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

        /*
         * FontFamily::getName, isMonospace and isVariable have a
         * "glib:get-property" attribute on Linux, but not on Windows and
         * macOS. We set the same attribute on all, so they are correctly
         * merged into one method in the Java bindings.
         */
        if (element instanceof Method m
                && List.of(
                        "pango_font_family_get_name",
                        "pango_font_family_is_monospace",
                        "pango_font_family_is_variable"
                ).contains(m.callableAttrs().cIdentifier()))
            return m.withAttribute("glib:get-property", "name");

        return element;
    }
}
