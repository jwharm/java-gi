/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2026 Jan-Willem Harmannij
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

package org.javagi.configuration;

import org.javagi.util.ModuleDataParser;

import java.util.*;

import static java.util.Objects.requireNonNullElse;

/**
 * The ModuleInfo class defines the mapping between GIR namespaces and Java
 * package names, the base URL for image links, and a short description of the
 * package.
 */
public final class ModuleInfo {

    /**
     * Information about one specific GIR namespace
     */
    public record Module(String moduleName, String repositoryName, String repositoryVersion,
                         String javaPackage, String javaModule, String mavenName,
                         String docUrlPrefix, String description) {}

    private static final Map<String, Module> INCLUDED_MODULES = new ModuleDataParser().parse();
    private static final Map<String, Module> ALL_MODULES = new HashMap<>(INCLUDED_MODULES);

    /**
     * Add information about a module.
     *
     * @param namespace    name of the GIR namespace
     * @param moduleName   official name of the module
     * @param packageName  name of the generated Java package
     * @param docUrlPrefix URL to be prefixed to hyperlinks in generated Javadoc
     * @param description  description of the generated Java package
     */
    public static void add(String namespace,
                           String moduleName,
                           String packageName,
                           String docUrlPrefix,
                           String description) {
        ALL_MODULES.put(namespace.toLowerCase(), new Module(
                namespace.toLowerCase(),
                requireNonNullElse(moduleName, ""),
                "",
                requireNonNullElse(packageName, ""),
                requireNonNullElse(moduleName, ""),
                "",
                requireNonNullElse(docUrlPrefix, ""),
                requireNonNullElse(description, "")
        ));
    }

    private static Module get(String namespace) {
        Module info = ALL_MODULES.get(namespace.toLowerCase());
        if (info == null)
            throw new NoSuchElementException("GIR namespace " + namespace + " not found in ModuleInfo");
        return info;
    }

    /**
     * Check whether this GIR namespace is included in the set of modules
     * published by Java-GI.
     */
    public static boolean isIncludedModule(String namespace) {
        return INCLUDED_MODULES.containsKey(namespace);
    }

    /**
     * Get the module name for the specified GIR namespace
     */
    public static String moduleName(String namespace) {
        return get(namespace).moduleName();
    }

    /**
     * Get the official name for the specified GIR namespace
     */
    public static String repositoryName(String namespace) {
        return get(namespace).repositoryName();
    }

    /**
     * Get the generated Java package name for the specified GIR namespace
     */
    public static String javaPackage(String namespace) {
        return get(namespace).javaPackage();
    }

    /**
     * Get the generated Java module name for the specified GIR namespace
     */
    public static String javaModule(String namespace) {
        return get(namespace).javaModule();
    }

    /**
     * Get the generated Maven group and artifact-name for the specified GIR
     * namespace
     */
    public static String mavenName(String namespace) {
        return get(namespace).mavenName();
    }

    /**
     * Get the URL prefix for hyperlinks in Javadoc generated for the specified
     * GIR namespace
     */
    public static String docUrlPrefix(String namespace) {
        return get(namespace).docUrlPrefix();
    }

    /**
     * Get the Java package description for the specified GIR namespace
     */
    public static String description(String namespace) {
        return get(namespace).description();
    }

    /**
     * Get the repository version for the specified GIR namespace
     */
    public static String repositoryVersion(String namespace) {
        return get(namespace).repositoryVersion();
    }

    /**
     * Get a list of all gir files (Name-Version) for the specified module
     */
    public static List<String> getGirFilesForModule(String moduleName) {
        List<String> repositories = new ArrayList<>();
        for (String name : ALL_MODULES.keySet()) {
            Module module = ALL_MODULES.get(name);
            if (module.moduleName().equals(moduleName))
                repositories.add(module.repositoryName() + "-" + module.repositoryVersion());
        }
        return repositories;
    }
}
