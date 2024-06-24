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

package io.github.jwharm.javagi.gir;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.jwharm.javagi.util.CollectionUtils.filter;

/**
 * This class contains a map of all loaded GIR repositories.
 */
public final class Library implements Serializable {

    private final Map<String, Repository> repositories = new ConcurrentHashMap<>();

    /**
     * Add a new GIR file to the Library, computing the model using the
     * provided parse method. All GIR files are only parsed once: if a
     * requested model was already parsed, the existing one is returned.
     *
     * @param  name name of the GIR repository
     * @param  parser method that will parse a GIR file and return a Repository
     * @return the Repository
     */
    public Repository computeIfAbsent(
            String name,
            java.util.function.Function<? super String, ? extends Repository> parser) {
        return repositories.computeIfAbsent(name, parser);
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
