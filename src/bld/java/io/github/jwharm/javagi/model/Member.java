/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.Numbers;

public class Member extends GirElement {

    public final String cIdentifier;
    final int value;
    final boolean usable;

    public Member(GirElement parent, String name, String cIdentifier, String value) {
        super(parent);
        this.cIdentifier = cIdentifier;
        if (name != null) {
            this.name = Conversions.prefixDigits(name);
        }
        int v;
        boolean u = true;
        try {
            v = Numbers.parseInt(value);
        } catch (NumberFormatException nfe) {
            v = 0;
            u = false;
            System.out.println("Skipping <member name=\"" + name + "\""
                    + " c:identifier=\"" + cIdentifier + "\""
                    + " value=\"" + value + "\""
                    + ">: Not an integer");
        }
        this.value = v;
        this.usable = u;
    }

    /**
     * Compare equality with another member of the same structure
     * @param other Another member
     * @return true iff both members share the same parent and have equal names
     */
    public boolean equals(Member other) {
        return (parent == other.parent) && name.equals(other.name);
    }
}
