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

import io.github.jwharm.javagi.gir.*;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.Patch;
import io.github.jwharm.javagi.util.Platform;

import java.util.List;

public class GLibPatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        if (!"GLib".equals(namespace))
            return element;

        /*
         * g_clear_error has attribute throws="1" but no gerror** parameter (or
         * any other parameters) in the gir file.
         */
        if (element instanceof Namespace ns)
            return remove(ns, Function.class, "name", "clear_error");

        /*
         * GPid is defined as gint on Unix vs gpointer on Windows. The
         * generated Java class is an int Alias, so we remove the Windows
         * support.
         */
        if (element instanceof Alias a && "Pid".equals(a.name())) {
            a.setPlatforms(Platform.LINUX | Platform.MACOS);
            return a;
        }

        /*
         * GVariant has a method "get_type" that clashes with the "getType()"
         * method that is generated in Java. Therefore, it is renamed to
         * "getVariantType()".
         */
        if (element instanceof Method m
                && "g_variant_get_type".equals(m.attrs().cIdentifier()))
            return m.withAttribute("name", "get_variant_type");

        /*
         * The functions "g_main_context_query" and "g_main_context_check" have
         * GPollFD[] parameters. Because the size of GPollFD is unknown, it is
         * not possible to allocate the array with the correct size. For this
         * reason, both methods are excluded.
         */
        if (element instanceof Record r && "MainContext".equals(r.name())) {
            for (var m : List.of("query", "check"))
                r = remove(r, Method.class, "name", m);
            return r;
        }

        return element;
    }
}
