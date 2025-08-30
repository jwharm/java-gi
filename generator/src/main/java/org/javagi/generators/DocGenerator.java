/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 the Java-GI developers
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

package org.javagi.generators;

import org.javagi.util.Javadoc;
import org.javagi.gir.*;

import static org.javagi.util.Conversions.toJavaIdentifier;

public class DocGenerator {

    private final Doc doc;
    private boolean firstTag = true;

    public DocGenerator(Doc doc) {
        this.doc = doc;
    }

    public String generate() {
        return generate(false);
    }

    public String generate(boolean signalDeclaration) {
        String contents = doc.text();
        if (contents == null || contents.isEmpty())
            return "";

        StringBuilder builder = new StringBuilder();

        // Convert docstring to javadoc
        String javadoc = new Javadoc().convert(doc);

        // Write docstring
        writeDoc(builder, javadoc, null);

        // Methods and functions
        if (doc.parent() instanceof Callable func
                && (! (doc.parent() instanceof Callback || doc.parent() instanceof Signal))) {

            // Param
            Parameters parameters = func.parameters();
            if (parameters != null) {
                for (Parameter p : parameters.parameters()) {
                    if (p.isUserDataParameter()
                            || p.isDestroyNotifyParameter()
                            || p.isArrayLengthParameter())
                        continue;
                    if (p.infoElements().doc() != null)
                        writeDoc(builder,
                                new Javadoc().convert(p.infoElements().doc()),
                                "@param " + (p.varargs() ? "varargs" : toJavaIdentifier(p.name())));
                }
            }

            // Return (except for constructors)
            if (! (doc.parent() instanceof Constructor c
                    && "new".equals(c.name()))) {
                ReturnValue rv = func.returnValue();
                if (rv != null && rv.infoElements().doc() != null)
                    writeDoc(builder,
                            new Javadoc().convert(rv.infoElements().doc()),
                            "@return");
            }

            // Throws
            if (func.callableAttrs().throws_())
                writeDoc(builder,
                        "GErrorException see {@link org.gnome.glib.GError}",
                        "@throws");
        }

        // Signals
        if (signalDeclaration && doc.parent() instanceof Signal signal) {
            if (signal.detailed()) writeDoc(builder, "the signal detail",
                    "@param detail");
            writeDoc(builder, "the signal handler",
                    "@param handler");
            writeDoc(builder,
                    "a signal handler ID to keep track of the signal connection",
                    "@return");
        }

        // Property setters
        if (doc.parent() instanceof Property p) {
            String identifier = toJavaIdentifier(p.name());
            writeDoc(builder,
                    identifier + " the value for the {@code " + p.name() + "} property",
                    "@param");
            writeDoc(builder,
                    "the {@code Builder} instance is returned, to allow method chaining",
                    "@return");
        }

        // Field setters
        if (doc.parent() instanceof Field f) {
            String identifier = toJavaIdentifier(f.name());
            writeDoc(builder,
                    identifier + " the value for the {@code " + f.name() + "} field",
                    "@param");
        }

        // Version/Since
        if (doc.parent() instanceof GirElement element
                && element.infoAttrs().version() != null)
            writeDoc(builder, element.infoAttrs().version(), "@since");

        // Deprecated
        if (doc.parent() instanceof GirElement element
                && element.infoAttrs().deprecated()
                && element.infoElements().docDeprecated() != null) {
            writeDoc(builder,
                    new Javadoc().convert(element.infoElements().docDeprecated()),
                    "@deprecated");
        }

        return builder.toString();
    }

    // Write documentation, with an optional tag, and escape backslashes
    private void writeDoc(StringBuilder builder, String javadoc, String tag) {
        // Write an empty line between the doc and the tags
        if (firstTag && tag != null) {
            if (!builder.isEmpty())
                builder.append("\n");
            firstTag = false;
        }

        int count = 0;
        for (String line : javadoc.trim().lines().toList()) {
            String escapedLine = line.replace("\\", "\\\\");
            if (count == 0 && tag != null)
                escapedLine = tag + " " + escapedLine;
            builder.append(escapedLine).append("\n");
            count++;
        }
    }
}
