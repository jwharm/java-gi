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

package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.util.Patch;
import io.github.jwharm.javagi.util.Platform;

import java.util.List;

public class HarfBuzzPatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        /*
         * The "_t" postfix from HarfBuzz types is removed.
         */
        if (element instanceof RegisteredType rt
                && rt.cType() != null
                && rt.cType().startsWith("hb_")
                && rt.name().endsWith("_t")) {
            String newName = rt.name().substring(0, rt.name().length() - 2);
            return rt.withAttribute("name", newName);
        }
        if (element instanceof Type t
                && t.cType() != null
                && t.cType().startsWith("hb_")
                && t.name().endsWith("_t")) {
            String newName = t.name().substring(0, t.name().length() - 2);
            return t.withAttribute("name", newName);
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

        return element;
    }
}
