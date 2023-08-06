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

public class Array extends GirElement {

    public final String cType, length, zeroTerminated, fixedSize;

    public Array(GirElement parent, String name, String cType, String length, String zeroTerminated, String fixedSize) {
        super(parent);
        this.name = name;
        this.cType = cType;
        this.length = length;
        this.zeroTerminated = zeroTerminated;
        this.fixedSize = fixedSize;
    }
    
    /**
     * Returns a String that will contain or retrieve the array size.
     */
    public String size(boolean upcall) {
        // fixed-size attribute: Return the value of the attribute
        if (fixedSize != null) {
            return fixedSize;
        }
        // the "length" attribute refers to another parameter, which contains the length
        if (length != null) {
            if (parent instanceof Parameter p) {
                Parameter lp = p.getParameterAt(length);
                if (lp != null) {
                    if (upcall && lp.isOutParameter()) {
                        return "_" + lp.name + "Out.get()";
                    }
                    if (lp.type != null && (lp.type.isPointer() || lp.isOutParameter())) {
                        return lp.name + ".get().intValue()";
                    }
                    if (lp.type != null && lp.type.isAliasForPrimitive()) {
                        return lp.name + ".getValue()";
                    }
                    return lp.name;
                }
            }
        }
        // Size is unknown
        return null;
    }
}
