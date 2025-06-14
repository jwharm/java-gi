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

import static org.javagi.util.CollectionUtils.*;
import static java.util.function.Predicate.not;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public abstract class GirElement implements Serializable, Node {

    private List<Node> children;
    private final Map<String, String> attributes;
    private Node parent;

    GirElement() {
        this(Collections.emptyMap(), Collections.emptyList());
    }

    GirElement(Map<String, String> attributes) {
        this(attributes, Collections.emptyList());
    }

    GirElement(List<Node> children) {
        this(Collections.emptyMap(), children);
    }

    GirElement(Map<String, String> attributes, List<Node> children) {
        this.attributes = attributes;
        this.children = children;
        for (Node c : children) c.setParent(this);
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public void setAttr(String key, String newValue) {
        attributes.put(key, newValue);
    }

    public String attr(String key) {
        return attributes.get(key);
    }

    public int attrInt(String key) {
        String value = attr(key);
        return value == null ? -1 : Integer.parseInt(value);
    }

    public boolean attrBool(String key, boolean defaultValue) {
        String value = attr(key);
        return value == null ? defaultValue : value.equals("1");
    }

    public InfoAttrs infoAttrs() {
        return new InfoAttrs(
                attrBool("introspectable", true),
                attrBool("deprecated", false),
                attr("deprecated-version"),
                attr("version"),
                Stability.from(attr("stability")));
    }

    public CallableAttrs callableAttrs() {
        return new CallableAttrs(
                attrBool("introspectable", true),
                attrBool("deprecated", false),
                attr("deprecated-version"),
                attr("version"),
                Stability.from(attr("stability")),
                attr("name"),
                attr("c:identifier"),
                attr("shadowed-by"),
                attr("shadows"),
                attrBool("throws", false),
                attr("moved-to"),
                attr("glib:async-func"),
                attr("glib:sync-func"),
                attr("glib:finish-func"));
    }

    public InfoElements infoElements() {
        return new InfoElements(
                findAny(children, DocVersion.class),
                findAny(children, DocStability.class),
                findAny(children, Doc.class),
                findAny(children, DocDeprecated.class),
                findAny(children, SourcePosition.class),
                filter(children, Attribute.class));
    }

    public Node parent() {
        return parent;
    }

    public Map<String, String> attributes() {
        return attributes;
    }

    public List<Node> children() {
        return children;
    }

    public Namespace namespace() {
        return parent.namespace();
    }

    /**
     * Change an attribute with the provided name to the provided value.
     *
     * @param  attrName the name of the attribute
     * @param  newValue the new value for the attribute
     * @param  <T> the element must be a GirElement
     * @return a new instance of the same type as {@code elem}, with the new
     *         attribute value
     */
    @SuppressWarnings("unchecked")
    public <T extends Node> T withAttribute(String attrName, String newValue) {
        attributes.put(attrName, newValue);
        return (T) this;
    }

    /**
     * Replace the child elements of the element with a new list
     *
     * @param  newChildren the new list of child elements
     * @param  <T> the element must be a GirElement
     * @return a new instance of the same type as {@code elem}, with the new
     *         child elements
     */
    public <T extends Node> T withChildren(GirElement... newChildren) {
        return withChildren(Arrays.asList(newChildren));
    }

    /**
     * Replace the child elements of the element with a new list. Any
     * {@code null} elements are removed from the list.
     *
     * @param  children the new list of child elements
     * @param  <T> the element must be a GirElement
     * @return a new instance of the same type as {@code elem}, with the new
     *         child elements
     */
    @SuppressWarnings("unchecked")
    public <T extends Node> T withChildren(List<Node> children) {
        this.children = children;
        for (var child : children)
            child.setParent(this);
        return (T) this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        GirElement that = (GirElement) o;
        return Objects.equals(withoutDocs(children),
                              withoutDocs(that.children))
                && Objects.equals(identifyingAttrs(attributes),
                                  identifyingAttrs(that.attributes));
    }

    @Override
    public int hashCode() {
        return Objects.hash(withoutDocs(children), identifyingAttrs(attributes));
    }

    // Exclude documentation nodes when comparing GIR elements
    private static List<Node> withoutDocs(List<Node> list) {
        return list.stream()
                .filter(not(Documentation.class::isInstance))
                .filter(not(Docsection.class::isInstance))
                .filter(not(SourcePosition.class::isInstance))
                .toList();
    }

    // Only include identifying attributes (names, types) when comparing GIR
    // elements
    private static Map<String, String> identifyingAttrs(Map<String, String> attributes) {
        var attrs = List.of("name", "type", "c:identifier", "c:type");
        return attributes.entrySet().stream()
                .filter(attr -> attrs.contains(attr.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public String toString() {
        return "%s %s %s".formatted(
                getClass().getSimpleName(),
                attributes(),
                children()
        );
    }
}
