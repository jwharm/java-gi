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

import io.github.jwharm.javagi.util.Patch;
import io.github.jwharm.javagi.util.Platform;
import io.github.jwharm.javagi.gir.GirElement;
import io.github.jwharm.javagi.gir.Namespace;

/**
 * FileDescriptorBased is an interface on Linux and a record on Windows.
 * This means it is not considered the same type in the GIR model, and is generated twice.
 * To prevent this, it is removed from the Windows GIR model, so only the interface remains.
 */
public class GioWindowsFileDescriptorBased implements Patch {

    @Override
    public GirElement patch(GirElement element) {
        if (element instanceof Namespace ns && ns.name().equals("Gio") && ns.platforms() == Platform.WINDOWS)
            return removeType(ns, "FileDescriptorBased");

        return element;
    }
}