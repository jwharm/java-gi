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

package org.javagi.metadata;

import org.javagi.gir.*;

import java.lang.Class;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
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
            logger.severe("Rule does not match anything");
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
        Pattern pattern;
        try {
            String regex = convertGlobToRegex(rule.glob());
            pattern = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            logger.severe("Cannot compile regex from rule '" + rule.glob() + "': " + e.getMessage());
            return result;
        }

        for (var node : nodes) {
            for (var child : node.children()) {
                // Recursively descent into the <parameters> node
                if (child instanceof Parameters)
                    result.addAll(matchRule(List.of(child), rule));

                // node name matches pattern?
                var name = child instanceof Boxed
                        ? child.attr("glib:name")
                        : child.attr("name");
                var matchesPattern = name != null && pattern.matcher(name).matches();

                if (matchesPattern) {
                    // node type matches selector?
                    if (rule.selector() == null || rule.selector().equals(getTagName(child.getClass()))) {
                        result.add(child);
                    }
                }
            }
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
        Node target = TypeReference.lookup(node.namespace(), to);

        if (target == null) {
            try {
                target = node.namespace().parent().library().lookupNamespace(to);
            } catch (Exception e) {
                logger.severe("Type or namespace '" + to + "' not found");
                return;
            }
        }

        source.children().remove(node);
        target.children().add(node);
        node.setParent(target);
    }

    /**
     * Convert a Gir class name to "element-name" format.
     *
     * @param cls the Gir class for which to convert the name
     * @return the Gir tag name
     */
    private String getTagName(Class<? extends Node> cls) {
        String name = cls.getSimpleName();
        return switch (name) {
            case "CInclude" -> "c:include";
            case "DocFormat" -> "doc:format";
            case "Boxed" -> "glib:boxed";
            case "Signal" -> "glib:signal";
            default -> formatTag(name);
        };
    }

    /**
     * Convert a string formatted like "FooBar" to "foo-bar" format.
     *
     * @param className the class name to convert
     * @return the Gir tag name
     */
    private String formatTag(String className) {
        var sb = new StringBuilder();
        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (i > 0 && Character.isUpperCase(c))
                sb.append('-');

            sb.append(Character.toLowerCase(c));
        }

        return sb.toString();
    }

    /*
     * The following code was copied from https://stackoverflow.com/a/17369948
     * The author specified that the code is public domain.
     */

    /*
     * Converts a standard POSIX Shell globbing pattern into a regular expression
     * pattern. The result can be used with the standard {@link java.util.regex} API to
     * recognize strings which match the glob pattern.
     * <p/>
     * See also, the POSIX Shell language:
     * http://pubs.opengroup.org/onlinepubs/009695399/utilities/xcu_chap02.html#tag_02_13_01
     *
     * @param pattern A glob pattern.
     * @return A regex pattern to recognize the given glob pattern.
     */
    private String convertGlobToRegex(String pattern) {
        StringBuilder sb = new StringBuilder(pattern.length());
        int inGroup = 0;
        int inClass = 0;
        int firstIndexInClass = -1;
        char[] arr = pattern.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            switch (ch) {
                case '\\':
                    if (++i >= arr.length) {
                        sb.append('\\');
                    } else {
                        char next = arr[i];
                        switch (next) {
                            case ',':
                                // escape not needed
                                break;
                            case 'Q':
                            case 'E':
                                // extra escape needed
                                sb.append('\\');
                            default:
                                sb.append('\\');
                        }
                        sb.append(next);
                    }
                    break;
                case '*':
                    if (inClass == 0)
                        sb.append(".*");
                    else
                        sb.append('*');
                    break;
                case '?':
                    if (inClass == 0)
                        sb.append('.');
                    else
                        sb.append('?');
                    break;
                case '[':
                    inClass++;
                    firstIndexInClass = i+1;
                    sb.append('[');
                    break;
                case ']':
                    inClass--;
                    sb.append(']');
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    if (inClass == 0 || (firstIndexInClass == i && ch == '^'))
                        sb.append('\\');
                    sb.append(ch);
                    break;
                case '!':
                    if (firstIndexInClass == i)
                        sb.append('^');
                    else
                        sb.append('!');
                    break;
                case '{':
                    inGroup++;
                    sb.append('(');
                    break;
                case '}':
                    inGroup--;
                    sb.append(')');
                    break;
                case ',':
                    if (inGroup > 0)
                        sb.append('|');
                    else
                        sb.append(',');
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }
}
