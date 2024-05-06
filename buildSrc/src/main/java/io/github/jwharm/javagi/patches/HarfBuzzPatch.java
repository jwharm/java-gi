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

import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.util.Patch;
import io.github.jwharm.javagi.util.Platform;

import java.util.List;

public class HarfBuzzPatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        /*
         * The "_t" postfix from HarfBuzz types is removed from all <type>
         * elements in all other repositories.
         */
        if (!"HarfBuzz".equals(namespace) && element instanceof Type t) {
            String name = t.name();
            if (name != null
                    && name.startsWith("HarfBuzz.")
                    && name.endsWith("t")) {
                String newName = name.substring(0, name.length() - 2);
                return t.withAttribute("name", newName);
            }
        }

        if (!"HarfBuzz".equals(namespace))
            return element;

        /*
         * The "_t" postfix is removed from the HarfBuzz repository itself:
         * - from the <type> elements
         * - from the type declarations (<class>, <record>, <interface>, ...)
         */
        if (element instanceof Type t) {
            String name = t.name();
            if (name != null && name.endsWith("_t")) {
                String newName = name.substring(0, name.length() - 2);
                return t.withAttribute("name", newName);
            }
        }
        if (element instanceof RegisteredType rt && rt.name().endsWith("_t")) {
            String newName = rt.name().substring(0, rt.name().length() - 2);
            element = rt.withAttribute("name", newName);
        }

        if (element instanceof Namespace ns) {
            /*
             * This function has different parameter attributes on macOS.
             */
            ns = remove(ns, Function.class, "name", "ot_tags_from_script_and_language");

            /*
             * This constant has type "language_t" which cannot be instantiated.
             */
            return remove(ns, Constant.class, "name", "LANGUAGE_INVALID");
        }

        /*
         * Some HarfBuzz enums are in the Windows GIR file defined as
         * bitfields. We change them back to enums.
         */
        var enums = List.of(
                "hb_aat_layout_feature_selector_t",
                "hb_aat_layout_feature_type_t",
                "hb_buffer_serialize_format_t",
                "hb_ot_layout_baseline_tag_t",
                "hb_ot_meta_tag_t",
                "hb_ot_metrics_tag_t",
                "hb_script_t",
                "hb_style_tag_t"
        );
        if (element instanceof Bitfield b
                && b.platforms() == Platform.WINDOWS
                && enums.contains(b.cType()))
            return new Enumeration(b.attributes(), b.children(), b.platforms());

        /*
         * Return type of function "hb_font_set_var_named_instance" is wrong
         * in the linux and windows gir files
         */
        if (element instanceof Type t
                && "guint".equals(t.name())
                && "unsigned".equals(t.cType()))
            return t.withAttribute("c:type", "unsigned int");

        return element;
    }
}
