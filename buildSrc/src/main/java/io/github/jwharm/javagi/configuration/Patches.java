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

package io.github.jwharm.javagi.configuration;

import io.github.jwharm.javagi.patches.*;
import io.github.jwharm.javagi.util.Patch;

import java.util.List;

/**
 * Defines the list of patches that are applied to all GIR elements.
 */
public class Patches {
    public static final List<Patch> PATCHES = List.of(
            new AdwPatch(),
            new GLibPatch(),
            new GioPatch(),
            new GObjectPatch(),
            new GstAudioPatch(),
            new GstBasePatch(),
            new GtkPatch(),
            new HarfBuzzPatch(),
            new PangoPatch(),
            new SoupPatch(),
            new WebKitPatch()
    );
}
