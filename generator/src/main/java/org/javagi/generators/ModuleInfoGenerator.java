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

import org.javagi.configuration.ModuleInfo;
import org.javagi.gir.Include;
import org.javagi.gir.Library;
import org.javagi.gir.Namespace;

import java.util.HashSet;
import java.util.Set;

import static java.util.function.Predicate.not;

public class ModuleInfoGenerator {

    private final Library library;
    private final Set<String> packageNames;
    private final StringBuilder builder;

    private final Set<String> exports = new HashSet<>();
    private final Set<String> requires = new HashSet<>();

    public ModuleInfoGenerator(Library library, Set<String> packageNames) {
        this.library = library;
        this.packageNames = packageNames;
        this.builder = new StringBuilder();
    }

    public String generate() {
        String name = null;
        exports.addAll(packageNames);

        for (String exported : library.getExported()) {
            Namespace ns = library.lookupNamespace(exported);

            // Get the name of the created module
            name = ModuleInfo.javaModule(ns.name());

            // Export all java-gi packages
            String modulePackageName = ModuleInfo.javaPackage(ns.name());
            exports.add(modulePackageName);

            // List the required modules
            ns.parent().includes().stream()
                    .map(Include::name)
                    .map(ModuleInfo::javaModule)
                    .forEach(requires::add);
        }

        if (name == null)
            throw new IllegalStateException("Unknown module name");

        builder.append("""
                module %s {
                    requires static java.compiler;
                    requires transitive org.jspecify;
                """.formatted(name));

        exports.stream().sorted().forEach(this::appendExports);
        requires.stream()
                .filter(not(name::equals))
                .filter(not(exports::contains))
                .sorted()
                .forEach(this::appendRequires);

        if (! "org.gnome.glib".equalsIgnoreCase(name))
            for (String pkg : packageNames)
                builder.append("    opens ").append(pkg).append(" to org.gnome.glib;\n");

        builder.append("}\n");
        return builder.toString();
    }

    private void appendRequires(String module) {
        builder.append("    requires transitive ").append(module).append(";\n");
    }

    private void appendExports(String packageName) {
        builder.append("    exports ").append(packageName).append(";\n");
    }
}
