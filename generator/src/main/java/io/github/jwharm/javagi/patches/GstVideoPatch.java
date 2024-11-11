/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 Jan-Willem Harmannij
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

import io.github.jwharm.javagi.gir.Callable;
import io.github.jwharm.javagi.gir.Function;
import io.github.jwharm.javagi.gir.GirElement;
import io.github.jwharm.javagi.util.Patch;

import java.util.List;

public class GstVideoPatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        if (!"GstVideo".equals(namespace))
            return element;

        /*
         * VideoInfo::fromCaps clashes with VideoInfo::newFromCaps because the
         * "new" prefix is removed in Java-GI. The same happens in
         * VideoInfoDmaDrm. Change the name to "withCaps".
         */
        if (element instanceof Function f
                && List.of("gst_video_info_from_caps",
                           "gst_video_info_dma_drm_from_caps")
                       .contains(f.callableAttrs().cIdentifier()))
            return f.withAttribute("name", "with_caps");

        return element;
    }
}
