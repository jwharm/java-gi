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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class GirElement implements Serializable {

    private final List<GirElement> children;
    private final Map<String, String> attributes;
    private GirElement parent;

    GirElement() {
        this(Collections.emptyMap(), Collections.emptyList());
    }

    GirElement(Map<String, String> attributes) {
        this(attributes, Collections.emptyList());
    }

    GirElement(List<GirElement> children) {
        this(Collections.emptyMap(), children);
    }

    GirElement(Map<String, String> attributes, List<GirElement> children) {
        this.attributes = attributes;
        this.children = children;
        for (GirElement c : children) c.parent = this;
    }

    protected String attr(String key) {
        return attributes.get(key);
    }

    protected int attrInt(String key) {
        String value = attr(key);
        return value == null ? -1 : Integer.parseInt(value);
    }

    protected boolean attrBool(String key, boolean defaultValue) {
        String value = attr(key);
        return value == null ? defaultValue : value.equals("1");
    }

    protected InfoAttrs infoAttrs() {
        return new InfoAttrs(attrBool("introspectable", true), attrBool("deprecated", false),
                attr("deprecated-version"), attr("version"), Stability.from(attr("stability")));
    }

    protected CallableAttrs callableAttrs() {
        return new CallableAttrs(attrBool("introspectable", true), attrBool("deprecated", false),
                attr("deprecated-version"), attr("version"), Stability.from(attr("stability")), attr("name"),
                attr("c:identifier"), attr("shadowed-by"), attr("shadows"), attrBool("throws", false),
                attr("moved-to"));
    }

    protected InfoElements infoElements() {
        return new InfoElements(attr("doc-version"), attr("doc-stability"), findAny(children, Doc.class),
                findAny(children, DocDeprecated.class), findAny(children, SourcePosition.class),
                filter(children, Attribute.class));
    }

    public GirElement parent() {
        return parent;
    }

    public Map<String, String> attributes() {
        return attributes;
    }

    public List<GirElement> children() {
        return children;
    }

    public Namespace namespace() {
        return parent.namespace();
    }

    @Override
    public String toString() {
        return "%s %s %s".formatted(getClass().getSimpleName(), attributes(), children());
    }
}
