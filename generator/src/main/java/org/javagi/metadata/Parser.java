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

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

import static org.javagi.metadata.TokenType.*;

/**
 * Parser for metadata files. The parser uses a MetadataScanner to tokenize the
 * metadata file contents, and builds a tree of metadata rules.
 * <p>
 * Grammar:
 * <pre>{@code
 * metadata ::= [ rule [ '\n' relativerule ]* ]
 * rule ::= pattern ' ' [ args ]
 * relativerule ::= '.' rule
 * pattern ::= glob [ '#' selector ] [ '.' pattern ]
 * }</pre>
 */
public class Parser {

    // A "root" rule can have multiple "relative" rules on the following lines.
    private enum Relation {
        ROOT,
        RELATIVE
    }

    // The output of this logger will be displayed by Gradle
    private static final Logger logger = Logger.getLogger(Parser.class.getName());

    // The metadata scanner
    private final Scanner scanner;

    // The last read token
    private Token token;

    /**
     * Create a parser for a metadata file for the provided Gir repository.
     *
     * @param name    the Gir repository name to parse metadata for
     * @param version the Gir repository version to parse metadata for
     */
    public Parser(String name, String version) {
        this.scanner = createScanner(name, version);
    }

    // Find metadata file, read the contents into a String, and create a Scanner
    private static Scanner createScanner(String name, String version) {
        String filename = "%s-%s.metadata".formatted(name, version);

        try (InputStream is = Parser.class.getResourceAsStream("/metadata/" + filename)) {
            if (is == null) // No metadata found for this repository
                return null;

            String contents = new String(is.readAllBytes());
            return new Scanner(filename, contents);
        } catch (IOException e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    /**
     * Parse the metadata file and create a list of rules. This function will
     * loop until the entire file has been parsed.
     * 
     * @return the rules list
     */
    public List<Rule> parse() {
        List<Rule> rules = new ArrayList<>();

        if (scanner == null)
            return rules;

        // Scan the first token
        next();

        while (true) {
            Rule rule = parseRule(Relation.ROOT);
            if (rule == null) break;
            rules.add(rule);
        }

        return rules;
    }

    // Parse a rule, with all rules below it. Return null at end of file.
    private Rule parseRule(Relation relation) {
        String glob;
        String selector = null;
        List<Rule> children = new ArrayList<>();
        Map<String, String> args = new HashMap<>();

        // Skip empty lines
        while (token.type() == NEW_LINE)
            next();

        // Skip leading '.'
        if (token.type() == DOT)
            next();

        // End of file?
        if (token.type() == EOF)
            return null;

        // Read glob pattern and convert it to a regex pattern
        expect(IDENTIFIER);
        glob = token.text();
        next();

        // Read #selector
        if (token.type() == HASH) {
            next();
            expect(IDENTIFIER);
            selector = token.text();
            next();
        }

        // Recursively parse rules on the same line
        if (token.type() == DOT) {
            children.add(parseRule(relation));
            return new Rule(glob, selector, args, children);
        }

        // Read argument names and values
        while (token.type() == IDENTIFIER) {
            String name = token.text();
            next();
            String value = null;
            if (token.type() == EQUAL) {
                next();
                expect(STRING, IDENTIFIER);
                value = token.text();
                next();
            }
            args.put(name, value);
        }

        // We should be at the end of the line by now
        expect(NEW_LINE, EOF);

        // Parse relative rules (starting with a dot) on following lines
        if (relation == Relation.ROOT) {
            while (true) {
                // Skip empty lines
                while (token.type() == NEW_LINE)
                    next();

                // Scan relative rule
                if (token.type() == DOT)
                    children.add(parseRule(Relation.RELATIVE));
                else
                    break;
            }
        }

        return new Rule(glob, selector, args, children);
    }

    // Scan the next token and store it in a global variable
    private void next() {
        token = scanner.next();
    }

    // Log an error if the token doesn't have any of the expected types
    private void expect(TokenType... expected) {
        for (TokenType type : expected) {
            if (token.type() == type) return;
        }
        error("Invalid token " + token.type() +
                ", expected one of: " + Arrays.toString(expected));
    }

    // Log an error message. The source location is included in the error.
    private void error(String message) {
        logger.severe("%s: %d: %s%n".formatted(
                scanner.filename, scanner.getLine(scanner.start), message));
    }
}
