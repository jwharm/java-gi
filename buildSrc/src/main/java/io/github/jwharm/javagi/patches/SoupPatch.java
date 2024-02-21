/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 the Jan-Willem Harmannij
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
import io.github.jwharm.javagi.gir.VirtualMethod;
import io.github.jwharm.javagi.util.Patch;

import java.util.Map;

public class SoupPatch implements Patch {

    @Override
    public GirElement patch(GirElement element, String namespace) {

        if (!"Soup".equals(namespace))
            return element;

        /*
         * Virtual method Auth.update() has a HashTable argument where the
         * invoker method has a String argument. Remove the "invoker"
         * attribute, so they are generated as two separate methods in Java.
         */
        if (element instanceof VirtualMethod vm
                && "update".equals(vm.name())
                && "Auth".equals(vm.parameters().instanceParameter().type().name()))
            return new VirtualMethod(
                    Map.of("name", "update"),
                    vm.children(),
                    vm.platforms()
            );

        return element;
    }
}
