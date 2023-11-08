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

import io.github.jwharm.javagi.generator.Platform;

import java.util.HashMap;
import java.util.Map;

public class Module {
    /**
     * Map to find repositories by their name
     */
    public final Map<String, Repository> repositories = new HashMap<>();

    /**
     * Map to find elements by their {@code c:identifier} attribute
     */
    public final Map<String, GirElement> cIdentifierLookupTable = new HashMap<>();

    /**
     * Map to find types by their {@code c:type} attribute
     */
    public final Map<String, RegisteredType> cTypeLookupTable = new HashMap<>();

    /**
     * Map to find parent types by a types qualified name
     */
    public final Map<String, String> superLookupTable = new HashMap<>();

    /**
     * The OS platform of this module
     */
    public final Platform platform;

    public Module(Platform platform) {
        this.platform = platform;
    }

    /**
     * Return the Namespace of the Repository with the given name,
     * or {@code null} if the Repository with this name is not found
     * @param repositoryName the name of the repository
     * @return the Namespace, or {@code null} if the repository was not found
     */
    public Namespace getNamespace(String repositoryName) {
        Repository repo = repositories.get(repositoryName);
        return repo == null ? null : repo.namespace;
    }

    /**
     * Loop through all type references in all repositories, find the
     * actual type instance in the parsed GI tree, and save a reference
     * to that GirElement.
     */
    public void link() {

        // Link all type references to the accompanying types
        for (Repository repository : repositories.values()) {
            GirElement element = repository;
            while (element != null) {

                if ((element instanceof Type t)
                        && (t.name != null)
                        && (! t.isPrimitive)
                        && (! t.name.equals("none"))
                        && (! t.name.equals("utf8"))
                        && (! t.name.equals("gpointer")
                        && (! t.name.equals("gconstpointer")))) {

                    Repository r = repositories.get(t.girNamespace);
                    if (r != null) {
                        t.girElementInstance = r.namespace.registeredTypeMap.get(t.name);
                        if (t.girElementInstance != null) {
                            t.girElementType = t.girElementInstance.getClass().getSimpleName();
                        }
                    }
                    // Redo the initialization, now that all repositories have loaded.
                    t.init(t.qualifiedName);
                }

                // Link length-parameters to the corresponding arrays
                if (element instanceof Array array) {
                    if (array.length != null && array.parent instanceof Parameter p) {
                        Parameter lp = p.getParameterAt(array.length);
                        lp.linkedArray = array;
                    }
                }

                // Link destroy-parameters to the corresponding parameter
                if (element instanceof Parameter param && param.destroy != null) {
                    Parameter destroyNotify = param.getParameterAt(param.destroy);
                    destroyNotify.linkedParameter = param;
                }

                element = element.next;
            }
        }

        // Link virtual methods to the accompanying methods
        linkVirtualMethods();

        // Create lookup tables
        createIdLookupTable();
        createCTypeLookupTable();
    }

    /**
     * Flag methods with a `va_list` argument so they will not be generated.
     * As of JDK 21, va_list arguments will be unsupported.
     */
    public void flagVaListFunctions() {
        for (Repository repository : repositories.values()) {
            // Methods, virtual methods and functions
            for (RegisteredType rt : repository.namespace.registeredTypeMap.values()) {
                for (Constructor method : rt.constructorList) {
                    flagVaListFunction(method);
                }
                for (Method method : rt.methodList) {
                    flagVaListFunction(method);
                }
                for (Method method : rt.virtualMethodList) {
                    flagVaListFunction(method);
                }
                for (Method method : rt.functionList) {
                    flagVaListFunction(method);
                }
            }
            // Global functions
            for (Method method : repository.namespace.functionList) {
                flagVaListFunction(method);
            }
        }
    }

    private void flagVaListFunction(Method method) {
        if (method.parameters != null) {
            for (Parameter parameter : method.parameters.parameterList) {
                if (parameter.type != null
                        && ("va_list".equals(parameter.type.cType) || "va_list*".equals(parameter.type.cType))) {
                    method.skip = true;
                    break;
                }
            }
        }
    }

    /**
     * Find classes that define methods and virtual methods with the same name and type signature.
     * When found, add cross-references between them.
     */
    private void linkVirtualMethods() {
        for (Repository repository : repositories.values()) {
            for (RegisteredType rt : repository.namespace.registeredTypeMap.values()) {
                for (Method method : rt.methodList) {
                    for (VirtualMethod vm : rt.virtualMethodList) {
                        if (method.getNameAndSignature().equals(vm.getNameAndSignature())) {
                            method.linkedVirtualMethod = vm;
                            vm.linkedMethod = method;
                            vm.skip = true;
                            break;
                        }
                    }
                }
                // Flag virtual methods in interfaces to not be generated
                if (rt instanceof Interface) {
                    for (VirtualMethod vm : rt.virtualMethodList) {
                        vm.skip = true;
                    }
                }
            }
        }
    }

    /**
     * Update {@code cIdentifierLookupTable} with current {@code repositoriesLookupTable}
     */
    private void createIdLookupTable() {
        cIdentifierLookupTable.clear();
        for (Repository repository : repositories.values()) {
            GirElement element = repository;
            while (element != null) {
                if (element instanceof Method m) {
                    cIdentifierLookupTable.put(m.cIdentifier, m);
                } else if (element instanceof Member m) {
                    cIdentifierLookupTable.put(m.cIdentifier, m);
                }
                element = element.next;
            }
        }
    }

    /**
     * Update {@code cTypeLookupTable} with current {@code repositoriesLookupTable}
     */
    private void createCTypeLookupTable() {
        cTypeLookupTable.clear();
        for (Repository gir : repositories.values()) {
            for (RegisteredType rt : gir.namespace.registeredTypeMap.values()) {
                cTypeLookupTable.put(rt.cType, rt);
            }
        }
    }
}
