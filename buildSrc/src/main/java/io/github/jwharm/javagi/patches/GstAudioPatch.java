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

import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.gir.GirElement;
import io.github.jwharm.javagi.gir.Property;
import io.github.jwharm.javagi.gir.VirtualMethod;
import io.github.jwharm.javagi.util.Patch;

public class GstAudioPatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        if (!"GstAudio".equals(namespace))
            return element;

        /*
         * Property "output-buffer-duration-fraction" has type "Gst.Fraction".
         * A Gst.Fraction cannot automatically be put into a GValue, so we
         * cannot generate a builder setter in Java.
         */
        if (element instanceof Class c
                && "AudioAggregator".equals(c.name()))
            return remove(c, Property.class,
                    "name", "output-buffer-duration-fraction");

        /*
         * Virtual method AudioSink::stop overrides BaseSink::stop but returns
         * void instead of boolean. This is not allowed in Java, so it is
         * removed from the Java bindings.
         */
        if (element instanceof Class c
                && "AudioSink".equals(c.name()))
            return remove(c, VirtualMethod.class, "name", "stop");

        return element;
    }
}
