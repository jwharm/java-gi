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

import static io.github.jwharm.javagi.util.CollectionUtils.*;

import java.util.List;
import java.util.Map;

public final class Repository extends GirElement {

    private Module module;

    public Repository(Map<String, String> attributes, List<GirElement> children) {
        super(attributes, children);
    }

    public Module module() {
        return this.module;
    }

    void setModule(Module module) {
        this.module = module;
    }

    public Namespace lookupNamespace(String name) {
        return module().lookupNamespace(name);
    }

    @Override
    public Namespace namespace() {
        return null;
    }

    public GirElement lookupCIdentifier(String cIdentifier) {
        return module().lookupCIdentifier(cIdentifier);
    }

    public String version() {
        return attr("version");
    }

    public String cIdentifierPrefix() {
        return attr("c:identifier-prefixes");
    }

    public String symbolPrefix() {
        return attr("c:symbol-prefixes");
    }

    public List<CInclude> cIncludes() {
        return filter(children(), CInclude.class);
    }

    public List<Package> packages() {
        return filter(children(), Package.class);
    }

    public List<Namespace> namespaces() {
        return filter(children(), Namespace.class);
    }

    public List<Include> includes() {
        return filter(children(), Include.class);
    }
}
