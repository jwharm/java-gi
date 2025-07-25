/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2025 Jan-Willem Harmannij
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

import org.javagi.gir.Node;
import org.javagi.gir.Parameters;
import org.javagi.gir.Repository;
import org.javagi.gir.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.lang.Class;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Parse a metadata file and update the attributes on the matching nodes in a
 * Gir repository.
 */
public class MetadataParser {

    // The output of this logger will be displayed by Gradle
    private static final Logger logger = Logger.getLogger(MetadataParser.class.getName());

    // Metadata filename and contents
    private String filename;
    private String contents;

    // The last read token
    private String token;

    // The current position to read the next token
    private int pos = 0;

    /**
     * Parse a metadata file and update the attributes on the matching nodes in
     * the Gir repository.
     *
     * @param repository the Gir repository to apply the metadata to
     */
    public void parse(Repository repository) {
        filename = "%s-%s.metadata".formatted(
                repository.namespace().name(),
                repository.namespace().version()
        );

        try (InputStream is = MetadataParser.class.getResourceAsStream("/metadata/" + filename)) {
            if (is == null) // No metadata found for this repository
                return;

            contents = new String(is.readAllBytes());
        } catch (IOException e) {
            error(-1, e.getMessage());
            return;
        }

        contents = contents
                .replace("\t", " ")
                .replace("\r\n", "\n")
                .replace("\r", "\n");

        parseMetadata(repository);
    }

    /**
     * Parse all identifiers in the metadata file and match them against the
     * gir namespace.
     */
    private void parseMetadata(Repository repository) {
        next(); // read first token
        do {
            parseIdentifier(repository.namespaces(), false);
        } while (token != null); // "token == null" means end of file
    }

    /**
     * Parse a metadata identifier and apply the attributes.
     * <p>
     * Metadata identifiers can be nested (Foo.bar.baz) and can contain
     * wildcards (*.bar.baz). The "nodes" parameter is the list of gir nodes
     * matched by the parent metadata identifier.
     */
    private void parseIdentifier(List<? extends Node> nodes, boolean isSubIdentifier) {
        // skip empty lines
        while ("\n".equals(token))
            next();

        if (token == null)
            return; // end of file

        if (".".equals(token)) {
            if (isSubIdentifier) {
                next();
            } else {
                error(pos, "Unexpected '%s', expected a pattern.", token);
                token = null;
                return;
            }
        }

        // Remember the current position for logging purposes
        int lastPos = pos + 1 - token.length();

        String identifier = token;
        String selector = null;
        if ("#".equals(next())) {
            selector = next();
            next();
        }

        List<Node> childNodes = Collections.emptyList();
        try {
            childNodes = matchIdentifier(nodes, identifier, selector);

            if (childNodes.isEmpty())
                warn(lastPos, "Rule '%s' does not match anything", identifier);

        } catch (PatternSyntaxException e) {
            warn(lastPos, "Invalid rule '%s': %s", identifier, e.getMessage());
        }

        if (token == null)
            return;

        if (!".\n".contains(token)) {
            parseAttributes(childNodes);
            if (isSubIdentifier)
                return;
        }

        // parse sub-identifiers on the same line (recursively)
        if (".".equals(token)) {
            next();
            parseIdentifier(childNodes, false);
        }
        // parse sub-identifiers on new lines (in a loop)
        else {
            while ("\n".equals(token) && ".".equals(peek())) {
                parseIdentifier(childNodes, true);
            }
        }
    }

    /**
     * Parse attributes and apply them to the gir nodes.
     */
    private void parseAttributes(List<? extends Node> nodes) {
        while (! (token == null || "\n".equals(token))) {
            String key = token;
            next();
            if ("=".equals(token)) {
                setAttribute(nodes, key, readValue());
                next();
            } else {
                setAttribute(nodes, key, "1");
            }
        }
    }

    /**
     * Read a literal value from an attribute.
     */
    private String readValue() {
        if (pos >= contents.length()) {
            error(pos - 1, "Missing attribute value");
            token = null;
            return null;
        }

        // string literal
        if (contents.charAt(pos) == '"') {
            int begin = ++pos;
            token = readToken("\"\n");

            // empty string
            if ("\"".equals(token)) {
                pos++;
                return "";
            }

            // end of line
            if (token == null || contents.charAt(pos + token.length()) == '\n') {
                error(begin, "Unclosed string literal");
                return null;
            }

            pos += token.length() + 1;
            return token;
        }

        // read until whitespace and update the current position
        token = readToken(" \n");
        if (token != null)
            pos += token.length();

        return token;
    }

    /**
     * Same as next() but don't update the global state
     *
     * @return the token that would be returned by next()
     */
    private String peek() {
        String previousToken = token;
        int previousPos = pos;
        String newToken = next();
        while ("\n".equals(newToken))
            newToken = next();
        token = previousToken;
        pos = previousPos;
        return newToken;
    }

    /**
     * Read the next token and update the current position.
     * Comments and spaces are ignored.
     */
    private String next() {
        String separators = ".#/=\" \n";

        // read the next token and update the current position
        token = readToken(separators);
        if (token != null)
            pos += token.length();

        // space: skip and return next token
        if (" ".equals(token))
            return next();

        // single line comment: skip and return "\n
        if ("/".equals(token) && "/".equals(readToken(separators))) {
            pos = contents.indexOf("\n", pos);
            token = pos == -1 ? null : "\n";
        }

        // multi line comment: skip and return next token
        if ("/".equals(token) && "*".equals(readToken(separators))) {
            pos = contents.indexOf("*/", pos) + 2;
            token = pos == 1 ? null : next();
        }

        return token;
    }

    /**
     * Returns the next token.
     * Tokens are strings, separated by the provided characters.
     * Both the tokens and separators are returned.
     * The current position is not updated.
     */
    private String readToken(String separators) {
        for (int end = pos; end < contents.length(); end++) {
            if (separators.indexOf(contents.charAt(end)) != -1) {
                if (pos == end) end++;
                return contents.substring(pos, end);
            }
        }

        // end of file
        return null;
    }

    /**
     * Match a metadata pattern against the child nodes of the provided nodes.
     */
    private List<Node> matchIdentifier(List<? extends Node> nodes, String pattern, String selector) {
        // Metadata uses "*" as a wildcard. Change it to a regex ".*?".
        String escapedPattern = pattern.replace("*", ".*?");

        var patternSpec = Pattern.compile(escapedPattern);
        var result = new ArrayList<Node>();
        for (var node : nodes) {
            for (var child : node.children()) {
                // Recursively descent into the <parameters> node
                if (child instanceof Parameters)
                    result.addAll(matchIdentifier(List.of(child), pattern, selector));

                // match all nodes?
                var matchesEverything = "*".equals(pattern);

                // node name matches pattern?
                var name = child.attr("name");
                var matchesPattern = name != null && patternSpec.matcher(name).matches();

                if (matchesEverything || matchesPattern) {
                    // node type matches selector?
                    if (selector == null || selector.equals(getTagName(child.getClass()))) {
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
        if (val == null) {
            error(pos - 1, "Unexpected end of file");
            return;
        }

        if ("\n".equals(val)) {
            error(pos - 1, "Unexpected end of line");
            return;
        }

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
                warn(pos, "Type or namespace '%s' not found", to);
                return;
            }
        }

        source.children().remove(node);
        target.children().add(node);
        node.setParent(target);
    }

    /**
     * Convert a Gir class name to "element-name" format.
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

    /**
     * Log a warning message. The source location is included in the error.
     */
    private void warn(int location, String message, Object... args) {
        logger.warning(getSourcePosition(location) + message.formatted(args));
    }

    /**
     * Log an error message. The source location is included in the error.
     */
    private void error(int location, String message, Object... args) {
        logger.severe(getSourcePosition(location) + message.formatted(args));
    }

    /**
     * Return a String with the filename and line number, for example
     * {@code "MyLib-2.0.metadata: 16: "} when the source position is on line
     * 16 of file "MyLib-2.0.metadata".
     */
    private String getSourcePosition(int pos) {
        if (pos < 0)
            return filename + ": ";

        int line = 1;
        for (int i = 0; i < pos; i++)
            if (contents.charAt(i) == '\n')
                line++;

        return filename + ": " + line + ": ";
    }
}
