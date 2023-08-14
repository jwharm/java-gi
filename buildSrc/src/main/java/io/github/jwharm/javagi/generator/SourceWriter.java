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

package io.github.jwharm.javagi.generator;

import java.io.IOException;
import java.io.Writer;

/**
 * Write Strings to a provided {@link Writer} instance, adding indentation
 * to each new line.
 */
public class SourceWriter implements AutoCloseable {

    private final Writer writer;
    private String indent = "";
    private boolean writeIndent = true;

    /**
     * Create a new SourceWriter with indentation level 0.
     * @param writer the writer to write String values to.
     */
    public SourceWriter(Writer writer) {
        this.writer = writer;
    }

    /**
     * Increase the indentation level by 4 spaces.
     */
    public void increaseIndent() {
        indent += "    ";
    }

    /**
     * Decrease the indentation level by 4 spaces.
     * @throws IndexOutOfBoundsException when indentation level was already 0
     */
    public void decreaseIndent() {
        indent = indent.substring(4);
    }

    /**
     * Write the provided String, but when {@code str} ends with a newline ({@code \n}),
     * the next line will be indented with the current indentation level.
     * <p>
     * If {@code str} is {@code null}, nothing is written.
     * @param str the String to write
     * @throws IOException thrown while calling {@link Writer#write(String)}
     */
    public void write(String str) throws IOException {
        if (str == null) {
            return;
        }
        
        if (writeIndent) {
            writer.write(indent);
            writeIndent = false;
        }

        writer.write(str);

        if (str.endsWith("\n")) {
            writeIndent = true;
        }
    }

    /**
     * Close the {@code Writer} instance
     * @throws IOException thrown while calling {@link Writer#close()}
     */
    @Override
    public void close() throws IOException {
        writer.close();
    }
    
    /**
     * Return the {@code toString} of the enclosed {@code Writer} instance
     * @return the result of calling {@link Writer#toString()} on the enclosed {@code Writer} instance
     */
    @Override
    public String toString() {
        return writer.toString();
    }
}
