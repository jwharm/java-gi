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

import static io.github.jwharm.javagi.util.CollectionUtils.filter;

public class Module implements Serializable {

    private final List<Repository> repositories = new ArrayList<>();
    private final Set<String> names = new HashSet<>();

    public List<Repository> repositories() {
        return repositories;
    }

    public boolean contains(String name) {
        return names.contains(name);
    }

    public void add(String name, Repository repo) {
        repositories.add(repo);
        names.add(name);
        repo.setModule(this);
    }

    public Namespace lookupNamespace(String name) {
        for (Repository repo : repositories)
            for (Namespace ns : repo.namespaces())
                if (name.equals(ns.name())) return ns;
        throw new NoSuchElementException("No namespace with name " + name);
    }

    public GirElement lookupCIdentifier(String cIdentifier) {
        for (Repository repo : repositories) {
            for (Namespace ns : repo.namespaces()) {
                for (AbstractCallable ct : filter(ns.children(), AbstractCallable.class))
                    if (cIdentifier.equals(ct.attr("c:identifier")))
                        return ct;
                for (RegisteredType rt : filter(ns.children(), RegisteredType.class))
                    for (AbstractCallable ct : filter(rt.children(), AbstractCallable.class))
                        if (cIdentifier.equals(ct.attr("c:identifier")))
                            return ct;
            }
        }
        return null;
    }
}
