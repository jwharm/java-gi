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
import io.github.jwharm.javagi.generator.Javadoc;
import io.github.jwharm.javagi.generator.SourceWriter;

import java.io.IOException;

public class Doc extends GirElement {
    public final String space;
    public String contents;

    public Doc(GirElement parent, String space) {
        super(parent);
        this.space = space;
    }

    public void generate(SourceWriter writer, boolean signalDeclaration) throws IOException {
        generate(writer, signalDeclaration, true);
    }

    public void generate(SourceWriter writer, boolean signalDeclaration, boolean fullComment) throws IOException {
        if (contents == null || contents.isEmpty()) {
            return;
        }

        if (fullComment) {
            writer.write("/**\n");
        }

        // Convert docstring to javadoc
        String javadoc = Javadoc.getInstance().convert(this);
        
        // Write docstring
        writeDoc(writer, javadoc, null);
        
        // Version
        if (parent instanceof RegisteredType rt) {
            if (rt.version != null) {
                writeDoc(writer, rt.version, "@version");
            }
        }
        
        // Methods and functions
        if (parent instanceof CallableType ct
                && (! (parent instanceof Callback || parent instanceof Signal))) {
            
            // Param
            Parameters parameters = ct.getParameters();
            if (parameters != null) {
                for (Parameter p : parameters.parameterList) {
                    if (p.isInstanceParameter() || p.isUserDataParameter()
                            || p.isDestroyNotifyParameter() || p.isArrayLengthParameter()) {
                        continue;
                    }
                    if (p.doc != null) {
                        String pJavadoc = Javadoc.getInstance().convert(p.doc);
                        writeDoc(writer, pJavadoc, "@param " + (p.varargs ? "varargs" : p.name));
                    }
                }
            }
            
            // Return (except for constructors)
            if (! (parent instanceof Constructor c && c.name.equals("new"))) {
                ReturnValue rv = ct.getReturnValue();
                if (rv != null && rv.doc != null) {
                    String rvJavadoc = Javadoc.getInstance().convert(rv.doc);
                    writeDoc(writer, rvJavadoc, "@return");
                }
            }
            
            // Exceptions
            if (parent instanceof Method m) {
                if ("1".equals(m.throws_)) {
                    writeDoc(writer, "GErrorException see {@link org.gnome.glib.GError}", "@throws");
                }

                if (m.doPlatformCheck()) {
                    String text = "UnsupportedPlatformException when run on a platform other than ";
                    boolean first = true;
                    for (var p : m.platforms) {
                        if (! first) {
                            text += " or ";
                        }
                        text += Conversions.toCamelCase(p.name, true);
                        first = false;
                    }
                    writeDoc(writer, text, "@throws");
                }
            }
        }
        
        // Signals
        if (signalDeclaration && parent instanceof Signal signal) {
            if (signal.detailed) {
                writeDoc(writer, "the signal detail", "@param detail");
            }
            writeDoc(writer, "the signal handler", "@param handler");
            writeDoc(writer, "a {@link SignalConnection} object to keep track of the signal connection", "@return");
        }
        
        // Deprecated
        if (parent instanceof Method m && "1".equals(m.deprecated)) {
            if (parent.docDeprecated != null) {
                String deprecatedJavadoc = Javadoc.getInstance().convert(parent.docDeprecated);
                writeDoc(writer, deprecatedJavadoc, "@deprecated");
            }
        }

        // Property setters
        if (parent instanceof Property p) {
            writeDoc(writer, p.name + " the value for the {@code " + p.propertyName + "} property", "@param");
            writeDoc(writer, "the {@code Build} instance is returned, to allow method chaining", "@return");
        }
        
        // Field setters
        if (parent instanceof Field f) {
            writeDoc(writer, f.name + " the value for the {@code " + f.name + "} field", "@param");
            writeDoc(writer, "the {@code Build} instance is returned, to allow method chaining", "@return");
        }

        if (fullComment) {
            writer.write(" */\n");
        }
    }

    // Write documentation, with an optional tag, starting each line with a " * ", and escape backslashes
    private void writeDoc(SourceWriter writer, String javadoc, String tag) throws IOException {
        
        // Write the lines (starting each line with " * ")
        int count = 0;
        for (String line : javadoc.trim().lines().toList()) {
            String escapedLine = line.replace("\\", "\\\\");
            
            // Write tag (optional)
            if (count == 0 && tag != null) {
                escapedLine = tag + " " + escapedLine;
            }
            writer.write(" * " + escapedLine + "\n");
            count++;
        }
    }
}
