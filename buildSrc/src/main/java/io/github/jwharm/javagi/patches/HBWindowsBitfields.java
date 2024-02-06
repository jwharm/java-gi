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

import io.github.jwharm.javagi.gir.Bitfield;
import io.github.jwharm.javagi.gir.Enumeration;
import io.github.jwharm.javagi.gir.GirElement;
import io.github.jwharm.javagi.util.Patch;
import io.github.jwharm.javagi.util.Platform;

import java.util.List;

/**
 * Some HarfBuzz enums are in the Windows GIR file defined
 * as bitfields. We change them back to enums.
 */
public class HBWindowsBitfields implements Patch {

    @Override
    public GirElement patch(GirElement node) {

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

        // HarfBuzz Windows GIR files have enums as bitfields
        if (node instanceof Bitfield b && b.platforms() == Platform.WINDOWS && enums.contains(b.cType())) {
            return new Enumeration(b.attributes(), b.children(), b.platforms());
        }

        return node;
    }
}
