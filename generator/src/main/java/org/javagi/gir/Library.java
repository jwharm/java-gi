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

package org.javagi.gir;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.javagi.util.CollectionUtils.filter;

/**
 * This class contains a map of all loaded GIR repositories.
 */
public final class Library implements Serializable {

    private final Map<String, Repository> repositories = new ConcurrentHashMap<>();
    private final Set<String> exported = new HashSet<>();

    /**
     * Add a GIR repository to the library
     *
     * @param name       the name of the gir file
     * @param repository the parsed repository
     */
    public void put(String name, Repository repository) {
        repositories.put(name, repository);
        repository.setLibrary(this);
    }

    /**
     * Get a repository from the library with the requested namespace name
     *
     * @param  name the name of the namespace
     * @return the repository, or {@code null} if not found
     */
    public Repository get(String name) {
        return repositories.get(name);
    }

    public boolean contains(String name) {
        return repositories.containsKey(name);
    }

    public void setExported(String name) {
        exported.add(name);
    }

    public Set<String> getExported() {
        return exported;
    }

    public Namespace lookupNamespace(String name) {
        for (Repository repo : repositories.values())
            for (Namespace ns : repo.namespaces())
                if (name.equals(ns.name()))
                    return ns;

        throw new NoSuchElementException("No namespace with name " + name);
    }

    public Node lookupCIdentifier(String cIdentifier) {
        for (Repository repo : repositories.values()) {
            for (Namespace ns : repo.namespaces()) {
                Node result = lookupCIdentifier(cIdentifier, ns);
                if (result != null)
                    return result;

                for (var rt : filter(ns.children(), RegisteredType.class)) {
                    result = lookupCIdentifier(cIdentifier, rt);
                    if (result != null)
                        return result;
                }
            }
        }
        return null;
    }

    private Node lookupCIdentifier(String cIdentifier, Node parent) {
        for (Callable c : filter(parent.children(), Callable.class))
            if (cIdentifier.equals(c.attr("c:identifier")))
                return c;

        for (Member m : filter(parent.children(), Member.class))
            if (cIdentifier.equals(m.attr("c:identifier")))
                return m;

        return null;
    }
}
