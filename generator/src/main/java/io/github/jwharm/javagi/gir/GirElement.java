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
import static java.util.function.Predicate.not;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public abstract class GirElement implements Serializable, Node {

    private final List<Node> children;
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
        var newAttrs = new HashMap<>(attributes());
        newAttrs.put(attrName, newValue);
        return (T) switch(this) {
            case Alias a             -> new Alias(newAttrs, a.children(), a.platforms());
            case Array a             -> new Array(newAttrs, a.children());
            case Attribute _         -> new Attribute(newAttrs);
            case Bitfield b          -> new Bitfield(newAttrs, b.children(), b.platforms());
            case Boxed b             -> new Boxed(newAttrs, b.children(), b.platforms());
            case Callback c          -> new Callback(newAttrs, c.children(), c.platforms());
            case CInclude _          -> new CInclude(newAttrs);
            case Class c             -> new Class(newAttrs, c.children(), c.platforms());
            case Constant c          -> new Constant(newAttrs, c.children(), c.platforms());
            case Constructor c       -> new Constructor(newAttrs, c.children(), c.platforms());
            case Doc d               -> new Doc(newAttrs, d.text());
            case Docsection d        -> new Docsection(newAttrs, d.children());
            case DocDeprecated d     -> d;
            case DocVersion d        -> d;
            case Enumeration e       -> new Enumeration(newAttrs, e.children(), e.platforms());
            case Field f             -> new Field(newAttrs, f.children());
            case Function f          -> new Function(newAttrs, f.children(), f.platforms());
            case FunctionInline f    -> f;
            case FunctionMacro f     -> f;
            case Implements _        -> new Implements(newAttrs);
            case Include _           -> new Include(newAttrs);
            case InstanceParameter i -> new InstanceParameter(newAttrs, i.children());
            case Interface i         -> new Interface(newAttrs, i.children(), i.platforms());
            case Member m            -> new Member(newAttrs, m.children());
            case Method m            -> new Method(newAttrs, m.children(), m.platforms());
            case MethodInline m      -> m;
            case Namespace n         -> new Namespace(newAttrs, n.children(), n.platforms(), n.sharedLibraries());
            case Package _           -> new Package(newAttrs);
            case Parameter p         -> new Parameter(newAttrs, p.children());
            case Parameters p        -> p;
            case Prerequisite _      -> new Prerequisite(newAttrs);
            case Property p          -> new Property(newAttrs, p.children(), p.platforms());
            case Record r            -> new Record(newAttrs, r.children(), r.platforms());
            case Repository r        -> new Repository(newAttrs, r.children());
            case ReturnValue r       -> new ReturnValue(newAttrs, r.children());
            case Signal s            -> new Signal(newAttrs, s.children(), s.platforms());
            case SourcePosition _    -> new SourcePosition(newAttrs);
            case Type t              -> new Type(newAttrs, t.children());
            case Union u             -> new Union(newAttrs, u.children(), u.platforms());
            case Varargs v           -> v;
            case VirtualMethod v     -> new VirtualMethod(newAttrs, v.children(), v.platforms());
            default                  -> throw new UnsupportedOperationException("Unsupported element: " + this);
        };
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
        var newChildren = children;
        // Filter null values from the list
        if (children.contains(null))
            newChildren = children.stream().filter(Objects::nonNull).toList();

        return (T) switch(this) {
            case Alias a             -> new Alias(a.attributes(), newChildren, a.platforms());
            case Array a             -> new Array(a.attributes(), newChildren);
            case Attribute a         -> a;
            case Bitfield b          -> new Bitfield(b.attributes(), newChildren, b.platforms());
            case Boxed b             -> new Boxed(b.attributes(), newChildren, b.platforms());
            case Callback c          -> new Callback(c.attributes(), newChildren, c.platforms());
            case CInclude c          -> c;
            case Class c             -> new Class(c.attributes(), newChildren, c.platforms());
            case Constant c          -> new Constant(c.attributes(), newChildren, c.platforms());
            case Constructor c       -> new Constructor(c.attributes(), newChildren, c.platforms());
            case Doc d               -> d;
            case Docsection d        -> new Docsection(d.attributes(), newChildren);
            case DocDeprecated d     -> d;
            case DocVersion d        -> d;
            case Enumeration e       -> new Enumeration(e.attributes(), newChildren, e.platforms());
            case Field f             -> new Field(f.attributes(), newChildren);
            case Function f          -> new Function(f.attributes(), newChildren, f.platforms());
            case FunctionInline f    -> f;
            case FunctionMacro f     -> f;
            case Implements i        -> i;
            case Include i           -> i;
            case InstanceParameter i -> new InstanceParameter(i.attributes(), newChildren);
            case Interface i         -> new Interface(i.attributes(), newChildren, i.platforms());
            case Member m            -> new Member(m.attributes(), newChildren);
            case Method m            -> new Method(m.attributes(), newChildren, m.platforms());
            case MethodInline m      -> m;
            case Namespace n         -> new Namespace(n.attributes(), newChildren, n.platforms(), n.sharedLibraries());
            case Package p           -> p;
            case Parameter p         -> new Parameter(p.attributes(), newChildren);
            case Parameters _        -> new Parameters(newChildren);
            case Prerequisite p      -> p;
            case Property p          -> new Property(p.attributes(), newChildren, p.platforms());
            case Record r            -> new Record(r.attributes(), newChildren, r.platforms());
            case Repository r        -> new Repository(r.attributes(), newChildren);
            case ReturnValue r       -> new ReturnValue(r.attributes(), newChildren);
            case Signal s            -> new Signal(s.attributes(), newChildren, s.platforms());
            case SourcePosition s    -> s;
            case Type t              -> new Type(t.attributes(), newChildren);
            case Union u             -> new Union(u.attributes(), newChildren, u.platforms());
            case Varargs v           -> v;
            case VirtualMethod v     -> new VirtualMethod(v.attributes(), newChildren, v.platforms());
            default                  -> throw new UnsupportedOperationException("Unsupported element: " + this);
        };
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
