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

package io.github.jwharm.javagi.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import io.github.jwharm.javagi.gir.*;

import java.lang.foreign.MemorySegment;
import java.util.Arrays;

/**
 * Small utility functions for converting between C and Java identifiers
 */
public class Conversions {

    /**
     * Convert "identifier_name" to "identifierName"
     */
    public static String toJavaIdentifier(String typeName) {
        return prefixDigits(replaceJavaObjectMethodNames(replaceKeywords(toCamelCase(typeName, false))));
    }

    /**
     * Convert "identifier_name" to "package.name.IdentifierName"
     */
    public static ClassName toJavaQualifiedType(String typeName, Namespace ns) {
        if (typeName.contains(".")) {
            var rt = TypeReference.get(ns, typeName);
            return toJavaQualifiedType(rt.name(), rt.namespace());
        }
        return ClassName.get(ns.packageName(), toJavaSimpleType(typeName, ns));
    }

    /**
     * Convert "identifier_name" to "IdentifierName"
     */
    public static String toJavaSimpleType(String typeName, Namespace ns) {
        if (typeName.contains(".")) {
            var rt = TypeReference.get(ns, typeName);
            return toJavaSimpleType(rt.name(), rt.namespace());
        }
        return prefixDigits(replaceKnownType(replaceKeywords(toCamelCase(typeName, true)), ns));
    }

    // Convert a "type_name" or "type-name" to "typeName" or "TypeName".
    public static String toCamelCase(String typeName, boolean startUpperCase) {
        if (typeName == null) return null;
        StringBuilder builder = new StringBuilder();
        boolean upper = startUpperCase;
        for (char c : typeName.toCharArray()) {
            if (c == '_' || c == '-') {
                upper = true;
            } else {
                builder.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return builder.toString();
    }

    public static String capitalize(String string) {
        if (string == null || string.isEmpty()) return string;
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    public static String uncapitalize(String string) {
        if (string == null || string.isEmpty()) return string;
        return string.substring(0, 1).toLowerCase() + string.substring(1);
    }

    // A type name starting with a digit is not allowed; prefix it with an underscore.
    private static String prefixDigits(String name) {
        return name == null ? null : (Character.isDigit(name.charAt(0)) ? "_" + name : name);
    }

    // For types that are reserved Java keywords, append an underscore.
    private static String replaceKeywords(String name) {
        final String[] keywords = {
                "abstract", "continue", "for", "new", "switch", "assert", "default", "goto", "package",
                "synchronized", "boolean", "do", "if", "private", "this", "break", "double", "implements",
                "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum",
                "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char",
                "final", "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile",
                "const", "float", "native", "super", "while", "wait", "finalize", "null"
        };
        return Arrays.asList(keywords).contains(name) ? name + "_" : name;
    }

    // For types that conflict with common Java classes, prefix with C namespace
    public static String replaceKnownType(String name, Namespace ns) {
        if (ns == null) return name;
        final String[] types = {"String", "Object", "Error", "Builder"};
        return Arrays.stream(types).anyMatch(kw -> kw.equalsIgnoreCase(name)) ? ns.cIdentifierPrefix() + name : name;
    }

    // Overriding java.lang.Object methods is not allowed in default methods (in interfaces),
    // so we append an underscore to those method names.
    private static String replaceJavaObjectMethodNames(String name) {
        for (java.lang.reflect.Method m : Object.class.getMethods()) {
            if (m.getName().equals(name)) {
                return name + "_";
            }
        }
        return name;
    }

    /**
     * Convert C type declaration into Java type declaration. Return {@code null} when conversion failed.
     */
    public static String toJavaBaseType(String name) {
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
            case "gtype" -> "org.gnome.glib.GType";
            case "long double" -> "double";
            case "valist", "va_list" -> "VaList";
            default -> null;
        };
    }

    public static TypeName primitiveTypeName(String primitive) {
        return switch(primitive) {
            case "byte" -> TypeName.BYTE;
            case "boolean" -> TypeName.BOOLEAN;
            case "char" -> TypeName.CHAR;
            case "double" -> TypeName.DOUBLE;
            case "float" -> TypeName.FLOAT;
            case "int" -> TypeName.INT;
            case "long" -> TypeName.LONG;
            case "short" -> TypeName.SHORT;
            default -> throw new IllegalStateException("Unexpected value: " + primitive);
        };
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
     * Get the type name to use when interfacing with native code
     */
    public static String getCarrierTypeString(AnyType t) {
        if (t == null || t instanceof Array)
            return "java.lang.foreign.MemorySegment";

        Type type = (Type) t;

        if (type.isPointer())
            return "java.lang.foreign.MemorySegment";

        if (type.isBoolean())
            return "int";

        if (type.isPrimitive() || "none".equals(t.cType()))
            return type.javaType();

        RegisteredType target = type.get();

        if (target instanceof FlaggedType)
            return "int";

        if (target instanceof Alias a && a.type().isPrimitive())
            return a.type().javaType();

        return "java.lang.foreign.MemorySegment";
    }

    public static TypeName getCarrierTypeName(AnyType t) {
        if (t == null || t instanceof Array)
            return TypeName.get(MemorySegment.class);

        Type type = (Type) t;

        if (type.isPointer())
            return TypeName.get(MemorySegment.class);

        if (type.isBoolean())
            return TypeName.INT;

        if (type.isPrimitive() || "none".equals(t.cType()))
            return type.typeName();

        RegisteredType target = type.get();

        if (target instanceof FlaggedType)
            return TypeName.INT;

        if (target instanceof Alias a && a.type().isPrimitive())
            return a.type().typeName();

        return TypeName.get(MemorySegment.class);
    }

    /**
     * Get the memory layout of this type. Pointer types are returned as ADDRESS.
     */
    public static String getValueLayout(AnyType anyType) {
        return switch (anyType) {
            case null -> "ADDRESS";
            case Array _ -> "ADDRESS";
            case Type t -> t.isPointer() ? "ADDRESS" : getValueLayoutPlain(t);
        };
    }

    /**
     * Get the memory layout of this type. Pointers to primitive types are treated as the actual type.
     */
    public static String getValueLayoutPlain(Type t) {
        if (t == null) {
            return "ADDRESS";
        }
        RegisteredType target = t.get();
        if (target instanceof FlaggedType || t.isBoolean()) {
            return "JAVA_INT";
        }
        if (t.isPrimitive()) {
            return "JAVA_" + t.javaType().toUpperCase();
        }
        if (target instanceof Alias a && a.type().isPrimitive()) {
            return getValueLayoutPlain(a.type());
        }
        return "ADDRESS";
    }

    /**
     * Generate the literal representation of the provided value, according
     * to the provided type.
     */
    public static String literal(TypeName type, String value) throws NumberFormatException {
        if (type.equals(TypeName.BOOLEAN)) return Boolean.valueOf(value).toString();
        if (type.equals(TypeName.BYTE)) return Numbers.parseByte(value).toString();
        if (type.equals(TypeName.CHAR)) return "'" + value + "'";
        if (type.equals(TypeName.DOUBLE)) return Double.valueOf(value) + "d";
        if (type.equals(TypeName.FLOAT)) return Float.valueOf(value) + "f";
        if (type.equals(TypeName.INT)) return Numbers.parseInt(value).toString();
        if (type.equals(TypeName.LONG)) return Numbers.parseLong(value) + "L";
        if (type.equals(TypeName.SHORT)) return Numbers.parseShort(value).toString();
        if (type.equals(TypeName.get(String.class))) return '"' + value.replace("\\", "\\\\") + '"';
        return value;
    }
}
