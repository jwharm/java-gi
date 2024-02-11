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
import io.github.jwharm.javagi.gir.Class;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.util.Patch;
import io.github.jwharm.javagi.util.Platform;

import java.util.List;

public class GioPatch implements Patch {

    @Override
    public GirElement patch(GirElement element) {
        /*
         * FileDescriptorBased is an interface on Linux and a record on
         * Windows. This means it is not considered the same type in the GIR
         * model, and is generated twice. To prevent this, it is removed from
         * the Windows GIR model, so only the interface remains.
         */
        if (element instanceof Namespace ns
                && "Gio".equals(ns.name())
                && ns.platforms() == Platform.WINDOWS)
            return removeType(ns, "FileDescriptorBased");

        /*
         * The method "g_data_input_stream_read_byte" overrides
         * "g_buffered_input_stream_read_byte", but with a different return
         * type. This is not allowed in Java. Therefore, the method in
         * BufferedInputStream is renamed from "read_byte" to "read_int".
         */
        if (element instanceof Method m
                && "g_buffered_input_stream_read_byte".equals(m.attrs().cIdentifier()))
            return m.withAttribute("name", "read_int");

        /*
         * The "get_type" method is already generated as a static method by
         * Java-GI. This clashes with the virtual method with the same name.
         * As a workaround, the virtual method is removed from the Java
         * bindings.
         */
        if (element instanceof Class c
                && "SocketControlMessage".equals(c.name()))
            return remove(c, VirtualMethod.class, "name", "get_type");

        /*
         * "g_async_initable_new_finish" is a method declaration in the
         * interface "AsyncInitable". It is meant to be implemented as a
         * constructor (in Java-GI it would become a static factory method).
         * However, Java does not allow a (non-static) method to be implemented
         * or overridden by a static method. The current solution is to remove
         * the method from the interface. It is still available in the
         * implementing classes.
        */
        if (element instanceof Interface i
                && "AsyncInitable".equals(i.name()))
            return remove(i, Method.class, "name", "new_finish");

        /*
         * GIO File stream classes declare virtual methods that are also
         * declared in the "Seekable" interface that they implement. This
         * causes issues because the interface methods are public in Java,
         * while a virtual method in a class is generated with protected
         * visibility. This is not allowed in Java. Therefore, we remove the
         * virtual methods from the classes. They are still callable using the
         * (default) method definitions in the "Seekable" interface.
         */
        if (element instanceof Class c
                && List.of(
                        "GFileInputStream",
                        "GFileOutputStream",
                        "GFileIOStream").contains(c.cType())) {
            for (var vm : List.of("tell", "seek", "can_truncate", "can_seek"))
                c = remove(c, VirtualMethod.class, "name", vm);
            return c;
        }

        /*
         * Let GIO stream classes implement AutoCloseable in Java.
         */
        if (element instanceof Class c
                && List.of(
                        "GIOStream",
                        "GInputStream",
                        "GOutputStream").contains(c.cType()))
            return c.withAttribute("java-gi-auto-closeable", "1");

        return element;
    }
}
