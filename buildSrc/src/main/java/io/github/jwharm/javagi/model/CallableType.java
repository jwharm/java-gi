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
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;

public interface CallableType {

    Parameters getParameters();
    void setParameters(Parameters ps);

    ReturnValue getReturnValue();
    void setReturnValue(ReturnValue rv);

    Doc getDoc();

    String getThrows();

    /**
     * This function is used to determine if memory is allocated to marshal
     * the call parameters or return value.
     * @return true when memory is allocated (for an array, a native string,
     *         an out parameter, or a pointer to a primitive value)
     */
    default boolean allocatesMemory() {
        if (getThrows() != null) {
            return true;
        }
        if (getReturnValue().allocatesMemory()) {
            return true;
        }
        if (getParameters() != null) {
            return getParameters().parameterList.stream().anyMatch(Parameter::allocatesMemory);
        }
        return false;
    }

    default boolean generateFunctionDescriptor(SourceWriter writer) throws IOException {
        ReturnValue returnValue = getReturnValue();
        Parameters parameters = getParameters();
        boolean isVoid = returnValue.type == null || "void".equals(returnValue.type.simpleJavaType);

        boolean first = true;
        boolean varargs = false;
        writer.write("FunctionDescriptor.");

        // Return value
        if (isVoid) {
            writer.write("ofVoid(");
        } else {
            writer.write("of(");
            writer.write(Conversions.getValueLayout(returnValue.type));
            if (parameters != null || this instanceof Signal) {
                writer.write(", ");
            }
        }

        // For signals, add the pointer to the source
        if (this instanceof Signal) {
            writer.write("ValueLayout.ADDRESS");
            if (parameters != null) {
                writer.write(", ");
            }
        }

        // Parameters
        if (parameters != null) {
            for (Parameter p : parameters.parameterList) {
                if (p.varargs) {
                    varargs = true;
                    break;
                }
                if (! first) {
                    writer.write(", ");
                }
                first = false;
                writer.write(Conversions.getValueLayout(p.type));
            }
        }

        // **GError parameter
        if (getThrows() != null) {
            if (! first) {
                writer.write(", ");
            }
            writer.write("ValueLayout.ADDRESS");
        }

        writer.write(")");
        return varargs;
    }
}
