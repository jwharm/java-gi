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

import io.github.jwharm.javagi.configuration.ModuleInfo;
import io.github.jwharm.javagi.gir.Include;
import io.github.jwharm.javagi.gir.Namespace;

import java.util.Set;

public class ModuleInfoGenerator {

    private final Namespace ns;
    private final Set<String> packageNames;
    private final StringBuilder builder;

    public ModuleInfoGenerator(Namespace ns, Set<String> packageNames) {
        this.ns = ns;
        this.packageNames = packageNames;
        this.builder = new StringBuilder();
    }

    public String generate() {
        builder.append("""
                module %s {
                    requires static java.compiler;
                    requires static org.jetbrains.annotations;
                """.formatted(ModuleInfo.packageName(ns.name())));

        ns.parent().includes().stream()
                .map(Include::name)
                .map(ModuleInfo::packageName)
                // A minimal set of FreeType bindings is included in the Cairo module
                .map(name -> name.replace("org.freedesktop.freetype", "org.freedesktop.cairo"))
                .forEach(this::requires);

        exports(ModuleInfo.packageName(ns.name()));
        packageNames.forEach(this::exports);

        builder.append("}\n");
        return builder.toString();
    }

    private void requires(String module) {
        builder.append("    requires transitive ").append(module).append(";\n");
    }

    private void exports(String packageName) {
        builder.append("    exports ").append(packageName).append(";\n");
    }
}
