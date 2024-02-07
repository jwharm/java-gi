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

package io.github.jwharm.javagi.generators;

import io.github.jwharm.javagi.gir.Namespace;
import io.github.jwharm.javagi.util.Conversions;
import io.github.jwharm.javagi.util.Platform;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PackageInfoGenerator {

    private final Namespace ns;
    private final StringBuilder builder;

    public PackageInfoGenerator(Namespace ns) {
        this.ns = ns;
        this.builder = new StringBuilder();
    }

    public String generate() {
        builder.append("""
                /**
                 * This package contains the generated bindings for %s.
                 * <p>
                """.formatted(ns.name()));

        if (ns.sharedLibrary() != null) {
            builder.append(" * The following native libraries are required and will be loaded:");

            for (String libraryName : ns.sharedLibrary().split(",")) {
                String fileName = libraryName;

                // Strip path from library name
                if (fileName.contains("/"))
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);

                // Strip extension from library name
                fileName = fileName.substring(0, fileName.lastIndexOf("."));

                builder.append(" {@code ").append(fileName).append("}");
            }
            builder.append("""
                     
                     * <p>
                    """);
        }
        builder.append(" * For namespace-global declarations, refer to the {@link ")
                .append(ns.typeName().simpleName())
                .append("} class documentation.\n");

        if (ns.platforms() < Platform.ALL)
            builder.append("""
                     * <p>
                     * This package is only available on %s.
                    """.formatted(Platform.toString(ns.platforms())));

        for (var docsection : ns.docsections()) {
            String name = Arrays.stream(docsection.name().split("_"))
                    .map(Conversions::capitalize)
                    .collect(Collectors.joining(" "));
            String javadoc = new DocGenerator(docsection.doc()).generate();
            builder.append(" * \n")
                    .append(" * <h2>").append(name).append("</h2>\n");

            javadoc.lines().forEach(line ->
                    builder.append(" * ").append(line).append("\n"));
        }

        builder.append("""
                 */
                package %s;
                """.formatted(ns.packageName()));

        return builder.toString();
    }
}
