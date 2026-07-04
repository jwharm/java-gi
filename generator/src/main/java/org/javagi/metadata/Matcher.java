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

package org.javagi.metadata;

import org.javagi.gir.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;

public class Matcher {

    // The output of this logger will be displayed by Gradle
    private static final Logger logger = Logger.getLogger(Matcher.class.getName());

    public void match(List<Rule> metadata, Repository repository) {
        for (Rule rule : metadata) {
            processRule(repository.children(), rule);
        }
    }

    private void processRule(List<Node> nodes, Rule rule) {
        // Create a list of all matching gir nodes for this rule
        var matchingNodes = matchRule(nodes, rule);

        // Log unused entries
        if (matchingNodes.isEmpty()) {
            logger.severe(rule.position() + ": Rule '" + rule.glob() + "' does not match anything");
        }

        // Update gir attributes from the metadata arguments.
        // When the argument has no value, default to "1" (i.e. boolean "true").
        for (var arg : rule.args().entrySet()) {
            String value = arg.getValue();
            if (value == null) value = "1";
            setAttribute (matchingNodes, arg.getKey(), value);
        }

        // Match relative rules against the matching gir nodes
        for (var relative_rule : rule.children()) {
            processRule (matchingNodes, relative_rule);
        }
    }

    private List<Node> matchRule(List<Node> nodes, Rule rule) {
        var result = new ArrayList<Node>();
        try {
            for (var node : nodes)
                result.addAll(node.matchRule(rule.glob(), rule.selector()));
        } catch (PatternSyntaxException e) {
            logger.severe("%s: Cannot compile regex from rule '%s': %s%n"
                    .formatted(rule.position(), rule.glob(), e.getMessage()));
        }
        return result;
    }

    /**
     * Set a gir attribute value in the provided gir nodes.
     * Attribute value "()" means null, and the attribute is removed.
     */
    private void setAttribute(List<? extends Node> nodes, String key, String val) {
        for (var node : nodes) {
            if ("java-gi-parent".equals(key))
                reparent(node, val);
            else if ("()".equals(val))
                node.attributes().remove(key);
            else
                node.attributes().put(key, applyPattern(node.attr(key), val));
        }
    }

    /**
     * Attributes are usually in the form of key=value, to replace the existing
     * value with a new string literal. But it can also be changed using a
     * {@code {{value}}} placeholder, such that rule {@code "foo_{{value}}"}
     * applied to input "abc" will result in "foo_abc".
     *
     * @param  original      the original attribute value
     * @param  argumentValue the value from the metadata file (could contain
     *                       the {@code {{value}}} placeholder)
     * @return the new attribute value
     */
    private String applyPattern(String original, String argumentValue) {
        final String PLACEHOLDER = "{{value}}";
        if (original == null || !argumentValue.contains(PLACEHOLDER))
            return argumentValue;
        else
            return argumentValue.replace(PLACEHOLDER, original);
    }

    /**
     * Move a Gir node to another parent node
     *
     * @param node the node to reparent
     * @param to the new parent node (must exist) and
     */
    private void reparent(Node node, String to) {
        Node source = node.parent();
        Namespace ns = node.namespace();
        Node target = TypeReference.lookup(node.namespace(), to);

        if (target == null && to.equals(ns.name()))
            target = ns;

        if (target == null) {
            logger.severe("Type or namespace '" + to + "' not found");
            return;
        }

        source.children().remove(node);
        target.children().add(node);
        node.setParent(target);
    }
}
