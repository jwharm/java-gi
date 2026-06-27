/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2026 the Java-GI developers
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

import org.javagi.javapoet.CodeBlock;
import org.javagi.util.Glob;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.javagi.util.CollectionUtils.tail;
import static org.javagi.util.Conversions.getTagName;

public interface Node {
    Namespace namespace();
    Node parent();
    void setParent(Node parent);

    List<Node> children();
    Map<String, String> attributes();

    String attr(String key);
    int attrInt(String key);
    boolean attrBool(String key, boolean defaultValue);

    <T extends Node> T withAttribute(String attrName, String newValue);
    <T extends Node> T withChildren(GirElement... newChildren);
    <T extends Node> T withChildren(List<Node> newChildren);

    List<CodeBlock> freeTextBlocks();

    default boolean skipJava() {
        return attrBool("java-gi-skip", false);
    }

    /**
     * Apply the predicate on this node and its children recursively, and
     * return whether a match was found.
     *
     * @param  predicate the predicate to apply
     * @param  skip      skip nodes of this type and their children
     * @return whether the predicate matched for this node or one of its
     *         children (recursively)
     */
    default boolean deepMatch(Predicate<? super Node> predicate, java.lang.Class<? extends Node> skip) {
        if (skip.isAssignableFrom(this.getClass()))
            return false;
        if (predicate.test(this))
            return true;
        return children().stream().anyMatch(c -> c.deepMatch(predicate, skip));
    }

    /**
     * Return a list of child nodes that match the provided glob and selector.
     *
     * @param glob     a pattern (POSIX Shell format) that the child node name must match
     * @param selector the required XML tag name
     * @return         a mutable list of all matching child nodes
     * @throws PatternSyntaxException when the glob cannot be compiled into a valid regex
     */
    default List<Node> matchRule(String glob, String selector) throws PatternSyntaxException {
        var result = new ArrayList<Node>();
        String regex = Glob.convertGlobToRegex(glob);
        Pattern pattern = Pattern.compile(regex);

        for (var child : children()) {
            // Recursively descent into the <parameters> node
            if (child instanceof Parameters)
                result.addAll(child.matchRule(glob, selector));

            // node name matches pattern?
            var name = child instanceof Boxed
                    ? child.attr("glib:name")
                    : child.attr("name");
            if (name == null) name = "";
            if (pattern.matcher(name).matches()) {
                // node type matches selector?
                if (selector == null || selector.equals(getTagName(child.getClass())))
                    result.add(child);
            }
        }

        return result;
    }

    default List<Node> select(String... patterns) {
        return select(List.of(patterns));
    }

    private List<Node> select(List<String> patterns) {
        // Leaf node: stop recursing
        if (patterns.isEmpty())
            return List.of(this);

        // Split "glob#selector" pattern
        String[] parts = patterns.getFirst().split("#", 2);
        String glob = parts[0];
        String selector = parts.length < 2 ? null : parts[1];

        // Recursively match the other patterns against the child nodes
        List<Node> results = new ArrayList<>();
        for (var child : matchRule(glob, selector))
            results.addAll(child.select(tail(patterns)));
        return results;
    }

    /**
     * Generate a path string with the type and name of this node and all
     * parent nodes. This can be useful for debug output.
     *
     * @return the path string
     */
    default String xpath() {
        String desc = getClass().getSimpleName() + (attr("name") == null ? "" : ("[" + attr("name") + "]"));
        return parent() == null ? desc : parent().xpath() + "/" + desc;
    }
}
