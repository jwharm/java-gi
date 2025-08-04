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

import java.util.logging.Logger;

import static org.javagi.metadata.TokenType.*;

/**
 * Lexical scanner for metadata files, loosely based on the Scanner example in
 * chapter 4 of the book "Crafting Interpreters" by Robert Nystrom.
 * <p>
 * The scanner reads an input string, and turns it into a stream of tokens. The
 * tokens are then processed by the parser, to build a list of metadata rules.
 * <p>
 * The scanner is designed to be used in a loop that requests new tokens by
 * calling {@link #next()}, until {@link TokenType#EOF} is returned.
 */
class Scanner {

    // The output of this logger will be displayed by Gradle
    private static final Logger logger = Logger.getLogger(Scanner.class.getName());

    // Metadata filename and contents
    final String filename;
    final String contents;
    
    // The start of the current token and the current position
    int start = 0;
    int current = 0;

    /**
     * Create a new lexical scanner for metadata files.
     *
     * @param filename the metadata filename, only used for logging purposes
     * @param contents the contents of the metadata file
     */
    Scanner(String filename, String contents) {
        this.filename = filename;
        this.contents = contents;
    }

    /**
     * Scan and return the next token. When there are no more tokens to read,
     * all subsequent calls will return an "end-of-file" token (with type
     * {@link TokenType#EOF}).
     *
     * @return the scanned token
     */
    Token next() {
        if (isAtEnd())
            return createToken(EOF);

        start = current;
        char c = advance();
        switch (c) {
            case ' ', '\t', '\r' -> {
                // skipped
                return next();
            }
            case '.' -> {
                return createToken(DOT);
            }
            case '=' -> {
                return createToken(EQUAL);
            }
            case '#' -> {
                return createToken(HASH);
            }
            case '\n' -> {
                return createToken(NEW_LINE);
            }
            case '"' -> {
                return scanString();
            }
            case '/' -> {
                if (match('/'))
                    skipSingleLineComment();
                else if (match('*'))
                    skipMultiLineComment();
                else
                    error("Unexpected character");
                return next();
            }
            default -> {
                if (isValid(c))
                    return scanIdentifier();
                error("Unexpected character");
                return next();
            }
        }
    }

    // End of file?
    private boolean isAtEnd() {
        return current >= contents.length();
    }

    // Return the next char, and update the current position
    private char advance() {
        return contents.charAt(current++);
    }

    // Return the next char, but don't update anything
    private char peek() {
        if (isAtEnd())
            return '\0';

        return contents.charAt(current);
    }

    // Advance only if the next char matches expectation
    private boolean match(char expected) {
        if (peek() != expected)
            return false;

        advance();
        return true;
    }

    // Read an IDENTIFIER token
    private Token scanIdentifier() {
        while (isValid(peek()))
            advance();

        return createToken(IDENTIFIER);
    }

    private boolean isValid(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               (c >= '0' && c <= '9') ||
                c == '(' || c == ')' ||
                c == '_' || c == '-' || c == ':' ||
                c == '?' || c == '*' || c == '{' || c == '}' || c == ',';
    }

    // Read a STRING token
    private Token scanString() {
        while (peek() != '"' && peek() != '\n' && !isAtEnd()) {
            char c = advance();
            if (c == '\\')
                match('"'); // handle escaped quotes
        }
        
        if (peek() == '\n' || isAtEnd())
            error("Unterminated string");
        else
            advance(); // The closing '"'

        // Trim the surrounding quotes
        String text = contents.substring(start + 1, current - 1);
        return new Token(STRING, text, start);
    }

    // Skip past a '// ...' comment
    private void skipSingleLineComment() {
        while (!isAtEnd() && peek() != '\n')
            advance();
    }

    // Skip past a  '/* ... */' comment
    private void skipMultiLineComment() {
        char c;
        do {
            if (isAtEnd())
                error("Unterminated comment");
            c = advance();
        } while (! (c == '*' && match('/')));
    }

    // Create a Token instance for the scanned characters
    private Token createToken(TokenType type) {
        return new Token(type, contents.substring(start, current), current);
    }

    // Log an error message. The source location is included in the error.
    private void error(String message) {
        logger.severe("%s: %d: %s%n".formatted(filename, getLine(start), message));
    }

    // Get the line number for the given position
    int getLine(int position) {
        int line = 1;
        for (int i = 0; i < position; i++)
            if (contents.charAt(i) == '\n')
                line++;

        return line;
    }
}
