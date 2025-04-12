/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 Jan-Willem Harmannij
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
import java.util.Map;

import static io.github.jwharm.javagi.util.CollectionUtils.listOfNonNull;
import static java.util.Collections.emptyList;

public class GLibPatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        if (!"GLib".equals(namespace))
            return element;

        if (element instanceof Namespace ns) {
            /*
             * GType was removed from GLib, but is still used
             * by `g_strv_get_type`
             */
            var gtype = new Alias(
                    Map.of("name", "Type", "c:type", "GType"),
                    List.of(new Type(Map.of("name", "gsize", "c:type", "gsize"),
                                     emptyList())),
                    ns.platforms());
            ns = add(ns, gtype);

            /*
             * These functions don't clean up the memory allocated for the
             * callback function. It is replaced in Java-GI by wrappers
             * around their respective "full" functions.
             */
            ns = remove(ns, Function.class, "name", "idle_add_once");
            ns = remove(ns, Function.class, "name", "timeout_add_once");
            ns = remove(ns, Function.class, "name", "timeout_add_seconds_once");

            /*
             * g_clear_error has attribute throws="1" but no gerror** parameter
             * (or any other parameters) in the gir file.
             */
            return remove(ns, Function.class, "name", "clear_error");
        }

        /*
         * GBytes is available in Java as a plain byte array. There are
         * functions in the Interop class to read, write and free GBytes.
         */
        if (element instanceof Record r && "Bytes".equals(r.name()))
            return r.withAttribute("java-gi-skip", "1");

        /*
         * GPid is defined as gint on Unix vs gpointer on Windows. The generated
         * Java class is an int Alias, so we remove the Windows support.
         */
        if (element instanceof Alias a && "Pid".equals(a.name())) {
            a.setPlatforms(Platform.LINUX | Platform.MACOS);
            return a;
        }

        /*
         * GThreadFunctions contains a virtual function pointer with a "gulong"
         * parameter, which can cause problems on Windows. GThreadFunctions is
         * deprecated, so we can safely remove the Windows support.
         */
        if (element instanceof Record r && "ThreadFunctions".equals(r.name())) {
            r.setPlatforms(Platform.LINUX | Platform.MACOS);
            return r;
        }

        /*
         * GVariant has a method "get_type" that clashes with the "getType()"
         * method that is generated in Java. Therefore, it is renamed to
         * "getVariantType()".
         */
        if (element instanceof Method m
                && "g_variant_get_type".equals(m.callableAttrs().cIdentifier()))
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

        /*
         * g_strfreev is nicely specified in the Gir file to take an array
         * of Strings. However, this makes Java-GI generate code to allocate
         * a new memory segment from a Java String[] parameter. So we patch
         * it to expect a pointer (a MemorySegment parameter in Java).
         */
        if (element instanceof Function f
                && "g_strfreev".equals(f.callableAttrs().cIdentifier())) {
            Parameter current = f.parameters().parameters().getFirst();
            Parameter replacement = current.withChildren(
                    current.infoElements().doc(),
                    new Type(Map.of("name", "gpointer", "c:type", "gpointer"),
                             emptyList()));
            return f.withChildren(
                    f.infoElements().doc(),
                    f.infoElements().sourcePosition(),
                    f.returnValue(),
                    f.parameters().withChildren(replacement));
        }

        /*
         * GLib.Strv is an alias for a String[], but it is defined as a type
         * with name="utf8" (though the c-type is "gchar**"). We change it to an
         * array.
         */
        if (element instanceof Alias a && "Strv".equals(a.name())) {
            return new Alias(a.attributes(),
                    listOfNonNull(a.infoElements().doc(),
                            a.infoElements().sourcePosition(),
                            new Array(Map.of("zero-terminated", "1"),
                                        List.of(new Type(Map.of("name", "utf8"),
                                                         emptyList())))),
                    a.platforms());
        }

        /*
         * GLib.ByteArray, GLib.HashTable, GLib.List and GLib.SList are not
         * generated from the gir data. Java-GI provides custom ByteArray,
         * HashTable, List and SList classes.
         */
        if (element instanceof Record r
                && List.of("ByteArray", "HashTable", "List", "SList")
                        .contains(r.name()))
            return r.withAttribute("java-gi-custom", "1");

        if (element instanceof Record r && "Variant".equals(r.name()))
            return r.withAttribute("java-gi-to-string", "print(true)");

        return element;
    }
}
