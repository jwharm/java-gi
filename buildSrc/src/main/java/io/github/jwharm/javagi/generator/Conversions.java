/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.generator;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.github.jwharm.javagi.configuration.PackageNames;
import io.github.jwharm.javagi.model.*;

/**
 * Utility functions to convert names and keywords
 */
public class Conversions {

    /**
     * Convert "Gdk" to "org.gnome.gdk"
     */
    public static String namespaceToJavaPackage(String ns) {
        return Objects.requireNonNullElse(PackageNames.getMap().get(ns), ns);
    }

    /** 
     * Convert "identifier_name" to "identifierName"
     */
    public static String toLowerCaseJavaName(String typeName) {
        return prefixDigits(replaceKeywords(toCamelCase(typeName, false)));
    }

    /**
     * Convert "identifier_name" to "IdentifierName"
     */
    public static String toUpperCaseJavaName(String typeName) {
        return prefixDigits(replaceKeywords(toCamelCase(typeName, true)));
    }

    /**
     * Convert "GLib.type_name" to "TypeName"
     */
    public static String toSimpleJavaType(String typeName, Namespace ns) {
        if (typeName == null) {
            return null;
        }
        int idx = typeName.indexOf('.');
        if (idx > 0) {
            return replaceKnownType(toCamelCase(typeName.substring(idx + 1), true), ns);
        } else {
            return replaceKnownType(toCamelCase(typeName, true), ns);
        }
    }

    /** 
     * Convert "Glib.type_name" to "org.gnome.glib.TypeName".
     * If the typeName does not contain a namespace, the name of the provided namespace {@code ns}
     * is prepended to the result. For example, {@code toQualifiedJavaType("button", "org.gnome.gtk")}
     * returns {@code "org.gnome.gtk.Button"}.
     */
    public static String toQualifiedJavaType(String typeName, Namespace ns) {
        String currentPackage = ns.packageName;
        if (typeName == null) {
            return null;
        }
        if (typeName.equals("VaList")) {
            return typeName;
        }
        int idx = typeName.indexOf('.');
        if (idx > 0) {
            String nsPrefix = typeName.substring(0, idx);
            String packageName = Conversions.namespaceToJavaPackage(nsPrefix);
            if (packageName == null) System.err.println("Could not get namespace for " + typeName);
            Namespace namespace = ns;
            Repository repo = ns.module().repositories.get(nsPrefix);
            if (repo != null) namespace = repo.namespace;
            return packageName + "." + replaceKnownType(toCamelCase(typeName.substring(idx + 1), true), namespace);
        } else {
            return currentPackage + "." + replaceKnownType(toCamelCase(typeName, true), ns);
        }
    }

    /**
     * Convert a "type_name" or "type-name" to "typeName" or "TypeName".
     * @param typeName the string to convert
     * @param startUpperCase if the result should start with an uppercase letter
     * @return the CamelCased string
     */
    public static String toCamelCase(String typeName, boolean startUpperCase) {
        if (typeName == null) {
            return null;
        }
        char[] chars = typeName.toCharArray();
        StringBuilder builder = new StringBuilder();

        boolean upper = startUpperCase;
        for (char c : chars) {
            if (c == '_' || c == '-') {
                upper = true;
            } else {
                builder.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return builder.toString();
    }

    /**
     * A type name starting with a digit is not allowed; prefix it with an underscore.
     */
    public static String prefixDigits(String name) {
        return name == null ? null : (Character.isDigit(name.charAt(0)) ? "_" + name : name);
    }

    /**
     * For types that are reserved Java keywords, append an underscore.
     */
    private static String replaceKeywords(String name) {
        final String[] keywords = {
                "abstract", "continue", "for", "new", "switch", "assert", "default", "goto", "package",
                "synchronized", "boolean", "do", "if", "private", "this", "break", "double", "implements",
                "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum",
                "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char",
                "final", "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile",
                "const", "float", "native", "super", "while", "wait", "finalize"
        };
        return Arrays.stream(keywords).anyMatch(kw -> kw.equalsIgnoreCase(name)) ? name + "_" : name;
    }

    /**
     * For types that conflict with common Java classes, prefix with C namespace
     */
    public static String replaceKnownType(String name, Namespace ns) {
        final String[] types = {"String", "Object", "Error", "Builder"};
        return Arrays.stream(types).anyMatch(kw -> kw.equalsIgnoreCase(name)) ? ns.cIdentifierPrefix + name : name;
    }

    /**
     * Overriding java.lang.Object methods is not allowed in default methods (in interfaces),
     * so we append an underscore to those method names.
     */
    public static String replaceJavaObjectMethodNames(String name) {
        for (java.lang.reflect.Method m : Object.class.getMethods()) {
            if (m.getName().equals(name)) {
                return name + "_";
            }
        }
        return name;
    }

    /**
     * Convert C type declaration into Java type declaration.
     */
    public static String convertToJavaType(String name, boolean qualified, Namespace ns) {
        return name == null ? null : switch (name.toLowerCase()) {
            case "gboolean" -> "boolean";
            case "gchar", "guchar", "gint8", "guint8" -> "byte";
            case "gshort", "gushort", "gint16", "guint16" -> "short";
            case "gint", "guint", "gint32", "guint32", "gunichar" -> "int";
            case "gint64", "gssize", "gsize", "goffset", "guint64", "gintptr", "guintptr", "glong", "gulong" -> "long";
            case "gdouble" -> "double";
            case "gfloat" -> "float";
            case "none" -> "void";
            case "utf8", "filename" -> "java.lang.String";
            case "gpointer", "gconstpointer" -> "java.lang.foreign.MemorySegment";
            case "gtype" -> qualified ? Conversions.toQualifiedJavaType("GLib.Type", ns) : Conversions.toSimpleJavaType("GLib.Type", ns);
            case "valist", "va_list" -> "VaList";
            case "long double" -> "double"; // unsupported data type
            default -> qualified ? Conversions.toQualifiedJavaType(name, ns) : Conversions.toSimpleJavaType(name, ns);
        };
    }

    /**
     * Get the type name to use when interfacing with native code
     */
    public static String getCarrierType(Type t) {
        if (t == null) {
            return "MemorySegment";
        } else if (t.isPointer()) {
            return "MemorySegment";
        } else if (t.isEnum() || t.isBitfield() || t.isBoolean()) {
            return "int";
        } else if (t.isPrimitive || "void".equals(t.simpleJavaType)) {
            return t.simpleJavaType;
        } else if (t.isAliasForPrimitive()) {
            return t.girElementInstance.type.simpleJavaType;
        } else {
            return "MemorySegment";
        }
    }

    /**
     * Get the memory layout of this type. Pointer types are returned as ValueLayout.ADDRESS.
     */
    public static String getValueLayout(Type t) {
        if (t == null || t.isPointer()) {
            return "ValueLayout.ADDRESS";
        } else {
            return getValueLayoutPlain(t);
        }
    }

    /**
     * Get the memory layout of this type. Pointers to primitive types are treated as the actual type.
     */
    public static String getValueLayoutPlain(Type t) {
        if (t == null) {
            return "ValueLayout.ADDRESS";
        }
        if (t.isEnum() || t.isBitfield() || t.isBoolean()) {
            return "ValueLayout.JAVA_INT";
        }
        if (t.isPrimitive) {
            if ("glong".equals(t.cType) || "gulong".equals(t.cType)) {
                return "ValueLayout.JAVA_INT";
            }
            return "ValueLayout.JAVA_" + t.simpleJavaType.toUpperCase();
        }
        if (t.isAliasForPrimitive()) {
            return getValueLayoutPlain(t.girElementInstance.type);
        }
        return "ValueLayout.ADDRESS";
    }

    /**
     * Returns true when this type is a Java primitive type
     */
    public static boolean isPrimitive(String javaType) {
        return javaType != null
                && List.of("boolean", "byte", "char", "double", "float", "int", "long", "short").contains(javaType);
    }
    
    /**
     * Convert "char" to "Character", "int" to "Integer", and uppercase all other primitive types
     */
    public static String primitiveClassName(String primitive) {
        return switch(primitive) {
            case "char" -> "Character";
            case "int" -> "Integer";
            default -> toCamelCase(primitive, true);
        };
    }

    /**
     * Generate the literal representation of the provided value, according 
     * to the provided type.
     */
    public static String literal(String type, String value) throws NumberFormatException {
        return switch (type) {
            case "boolean" -> Boolean.valueOf(value).toString();
            case "byte" -> Numbers.parseByte(value).toString();
            case "char" -> "'" + value + "'";
            case "double" -> Double.valueOf(value) + "d";
            case "float" -> Float.valueOf(value) + "f";
            case "int" -> Numbers.parseInt(value).toString();
            case "long" -> Numbers.parseLong(value) + "L";
            case "short" -> Numbers.parseShort(value).toString();
            case "java.lang.String" -> '"' + value.replace("\\", "\\\\") + '"';
            default -> value;
        };
    }
}
