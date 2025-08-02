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

/**
 * The types of tokens that are read by the metadata scanner.
 */
enum TokenType {
    /**
     * A {@code .} character
     */
    DOT,

    /**
     * A {@code =} character
     */
    EQUAL,

    /**
     * A {@code #} character
     */
    HASH,

    /**
     * A pattern (glob), selector, argument name or argument value.
     * <p>
     * Valid characters for an identifier:
     * <ul>
     *   <li> Alphabetic letters (upper and lower case)
     *   <li> Digits
     *   <li> Parens {@code ()}, to denote an empty/unset argument
     *   <li> Underscore {@code _}, dash {@code -} and semicolon {@code :}
     *   <li> Glob wildcards {@code ?} and {@code *}, and groups {@code {,}}
     * </ul>
     */
    IDENTIFIER,

    /**
     * A double-quoted string. The quotes are not included in the
     * {@link Token#text()}
     */
    STRING,

    /**
     * A {@code ' '} or {@code \t}
     * 
     * @see Scanner#setSignificantWhitespace
     */
    WHITESPACE,

    /**
     * A {@code \n} character
     */
    NEW_LINE,

    /**
     * End of file
     */
    EOF
}
