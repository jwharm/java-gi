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

import io.github.jwharm.javagi.gir.GirElement;
import io.github.jwharm.javagi.gir.Method;
import io.github.jwharm.javagi.gir.Parameters;
import io.github.jwharm.javagi.gir.Record;
import io.github.jwharm.javagi.gir.VirtualMethod;
import io.github.jwharm.javagi.util.Patch;

import java.util.List;

public class GstBasePatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        if (!"GstBase".equals(namespace))
            return element;

        /*
         * Aggregator::peekNextSample is a virtual method with an invoker
         * method, but the name of the "pad" parameter is "aggregatorPad".
         * We rename the parameter to "pad" so it is the same.
         */
        if (element instanceof VirtualMethod vm
                && "peek_next_sample".equals(vm.name())
                && "Aggregator".equals(vm.parameters()
                                         .instanceParameter()
                                         .type().name()))
            return new VirtualMethod(
                    vm.attributes(),
                    List.of(
                            vm.infoElements().doc(),
                            vm.infoElements().sourcePosition(),
                            vm.returnValue(),
                            new Parameters(List.of(
                                    vm.parameters().instanceParameter(),
                                    vm.parameters().parameters().getFirst()
                                            .withAttribute("name", "pad")
                            ))
                    ),
                    vm.platforms()
            );

        /*
         * Virtual method BaseSrc::query and BaseSink::query would be
         * protected in Java, but they override a public method with the same
         * name in Element. Therefore, they must also be public.
         */
        if (element instanceof VirtualMethod vm
                && "query".equals(vm.name())
                && List.of("BaseSrc", "BaseSink").contains(
                            vm.parameters().instanceParameter().type().name()))
            return vm.withAttribute("java-gi-override-visibility", "PUBLIC");

        /*
         * ByteReader::dupStringUtf16 has a parameter with an array of int16
         * values, but the size is unspecified, so we cannot convert this to a
         * short[] array. Remove it from the Java bindings.
         */
        if (element instanceof Record r
                && "ByteReader".equals(r.name()))
            return remove(r, Method.class, "name", "dup_string_utf16");

        return element;
    }
}
