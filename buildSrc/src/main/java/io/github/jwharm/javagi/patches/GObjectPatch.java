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

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GObjectPatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        if (!"GObject".equals(namespace))
            return element;

        if (element instanceof Namespace ns) {
            /*
             * VaList parameters are excluded from the Java bindings.
             * Therefore, the VaList marshaller classes and the
             * "signal_set_va_marshaller" function are excluded too.
             */
            ns = remove(ns, Callback.class, "name", "VaClosureMarshal");
            ns = remove(ns, Alias.class, "name", "SignalCVaMarshaller");
            ns = remove(ns, Function.class, "name", "signal_set_va_marshaller");
            return ns;
        }

        /*
         * `TYPE_FLAG_RESERVED_ID_BIT` is defined as GType but that doesn't
         * make sense in the Java bindings. Change it to a numeric type.
         */
        if (element instanceof Constant c
                && "TYPE_FLAG_RESERVED_ID_BIT".equals(c.name())) {
            Type type = new Type(
                    Map.of("name", "gsize", "c:type", "gsize"),
                    Collections.emptyList()
            );
            return c.withChildren(
                    c.infoElements().doc(),
                    c.infoElements().sourcePosition(),
                    type);
        }

        /*
         * GLib and GObject both define gtype as an alias to gsize. We replace
         * the gtype declaration in GObject with an alias for the GLib gtype,
         * so it will inherit in Java and the instances of both classes can be
         * used interchangeably in many cases.
         */
        if (element instanceof Alias a && "Type".equals(a.name())) {
            Type type = new Type(
                    Map.of("name", "GLib.Type", "c:type", "gtype"),
                    Collections.emptyList()
            );
            return a.withChildren(a.infoElements().doc(), type);
        }

        /*
         * The method "g_type_module_use" overrides "g_type_plugin_use", but
         * with a different return type. This is not allowed in Java.
         * Therefore, it is renamed from "use" to "use_type_module".
         */
        if (element instanceof Method m
                && "g_type_module_use".equals(m.callableAttrs().cIdentifier()))
            return m.withAttribute("name", "use_type_module");

        /*
         * Make GWeakRef generic (replacing all GObject arguments with generic
         * type {@code <T extends GObject>}).
         */
        if (element instanceof Record r && "WeakRef".equals(r.name()))
            return r.withAttribute("java-gi-generic", "1");

        /*
         * Change GInitiallyUnownedClass struct to refer to GObjectClass. Both
         * structs are identical, so this has no practical consequences,
         * besides convincing the bindings generator that
         * GObject.InitiallyUnownedClass is not a fundamental type class, but
         * extends GObject.ObjectClass.
         */
        if (element instanceof Record r
                && "InitiallyUnownedClass".equals(r.name())) {
            Type type = new Type(
                    Map.of("name", "GObject.ObjectClass",
                           "c:type", "GObjectClass"),
                    Collections.emptyList()
            );
            Field field = new Field(
                    Map.of("name", "parent_class"),
                    List.of(type)
            );
            return r.withChildren(r.infoElements().doc(), field);
        }

        /*
         * CClosure construction functions return floating references. There's
         * specific code in java-gi to ref_sink GInitiallyUnowned and GVariant,
         * but as CClosure shouldn't be used from Java anyway, we remove these
         * functions.
         */
        if (element instanceof Record r
                && "CClosure".equals(r.name())) {
            for (var fun : r.functions())
                if (fun.name().startsWith("new"))
                    r = remove (r, Function.class, "name", fun.name());
            return r;
        }

        /*
         * GObject.notify() is defined as a virtual method with an invoker
         * method, but the parameters are different. Remove the invoker
         * attribute, so they will be treated as separate methods.
         */
        if (element instanceof VirtualMethod vm
                && "notify".equals(vm.name())
                && "Object".equals(vm.parameters()
                                     .instanceParameter()
                                     .type().name()))
            return new VirtualMethod(
                    Map.of("name", "notify"),
                    vm.children(),
                    vm.platforms()
            );

        return element;
    }
}
