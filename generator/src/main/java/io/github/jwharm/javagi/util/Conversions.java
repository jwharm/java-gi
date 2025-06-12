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

package io.github.jwharm.javagi.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import io.github.jwharm.javagi.configuration.ModuleInfo;
import io.github.jwharm.javagi.gir.*;

import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.stream.Stream;

/**
 * Utility functions for converting between C and Java identifiers.
 */
public class Conversions {

    /**
     * Convert "identifier_name" to "identifierName"
     */
    public static String toJavaIdentifier(String typeName) {
        return prefixDigits(replaceKeywords(toCamelCase(typeName, false)));
    }

    /**
     * Convert "identifier_name" to "package.name.IdentifierName"
     */
    public static ClassName toJavaQualifiedType(String typeName, Namespace ns) {
        if (typeName == null) return null;
        if (typeName.contains(".")) {
            var rt = TypeReference.lookup(ns, typeName);
            return toJavaQualifiedType(rt.name(), rt.namespace());
        }
        return ClassName.get(
                ModuleInfo.packageName(ns.name()),
                toJavaSimpleType(typeName, ns));
    }

    /**
     * Convert "identifier_name" to "IdentifierName"
     */
    public static String toJavaSimpleType(String typeName, Namespace ns) {
        if (typeName.contains(".")) {
            var rt = TypeReference.lookup(ns, typeName);
            return toJavaSimpleType(rt.name(), rt.namespace());
        }
        return prefixDigits(replaceKnownType(
                replaceKeywords(toCamelCase(typeName, true)), ns));
    }

    /**
     * Prefix the name with an underscore if it starts with a digit
     */
    public static String toJavaConstant(String name) {
        return prefixDigits(name);
    }

    /**
     * Prefix the name with an underscore if it starts with a digit, and
     * returns the result upper-cased
     */
    public static String toJavaConstantUpperCase(String name) {
        return prefixDigits(name.toUpperCase());
    }

    /**
     * Convert a "type_name" or "type-name" to "typeName" or "TypeName".
     */
    public static String toCamelCase(String typeName, boolean startUpperCase) {
        if (typeName == null) return null;
        StringBuilder builder = new StringBuilder();
        boolean upper = startUpperCase;
        for (char c : typeName.toCharArray()) {
            if ((c == '_' || c == '-') && !builder.isEmpty()) {
                upper = true;
            } else {
                builder.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return builder.toString();
    }

    /**
     * Return the string with the first character upper-cased
     */
    public static String capitalize(String string) {
        if (string == null || string.isEmpty())
            return string;

        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    /**
     * Return the string with the first character lower-cased
     */
    public static String uncapitalize(String string) {
        if (string == null || string.isEmpty())
            return string;

        return string.substring(0, 1).toLowerCase() + string.substring(1);
    }

    /*
     * A type name starting with a digit is not allowed; prefix it with an
     * underscore.
     */
    private static String prefixDigits(String name) {
        return name == null ? null
                : (Character.isDigit(name.charAt(0)) ? "_" + name : name);
    }

    // For types that are reserved Java keywords, append an underscore.
    private static String replaceKeywords(String name) {
        final List<String> keywords = List.of(
                "abstract", "continue", "for", "new", "switch", "assert",
                "default", "goto", "package", "synchronized", "boolean", "do",
                "if", "private", "this", "break", "double", "implements",
                "protected", "throw", "byte", "else", "import", "public",
                "throws", "case", "enum", "instanceof", "return", "transient",
                "catch", "extends", "int", "short", "try", "char", "final",
                "interface", "static", "void", "class", "finally", "long",
                "strictfp", "volatile", "const", "float", "native", "super",
                "while", "wait", "finalize", "null",
                "handle" // used by java-gi
        );
        return keywords.contains(name) ? name + "_" : name;
    }

    /**
     * For types that conflict with common Java classes, prefix with C
     * namespace.
     */
    public static String replaceKnownType(String name, Namespace ns) {
        if (ns == null)
            return name;

        return Stream.of("String", "Object", "Error", "Builder")
                .anyMatch(kw -> kw.equalsIgnoreCase(name))
                        ? ns.cIdentifierPrefix() + name
                        : name;
    }

    /**
     * Convert C type declaration into Java type declaration. Return
     * {@code null} when conversion failed.
     */
    public static String toJavaBaseType(String name) {
        return name == null ? null : switch (name.toLowerCase()) {
            case "gboolean" -> "boolean";
            case "gchar", "guchar", "gint8", "guint8" -> "byte";
            case "gshort", "gushort", "gint16", "guint16" -> "short";
            case "gint", "guint", "gint32", "guint32", "gunichar",
                 // assume 32-bit uid_t, gid_t and socklen_t
                 "uid_t", "gid_t", "socklen_t" -> "int";
            case "gint64", "gssize", "gsize", "goffset", "guint64",
                 "gintptr", "guintptr", "glong", "gulong",
                 // assume 64-bit time_t, dev_t, off_t and pid_t
                 "time_t", "dev_t", "off_t", "pid_t" -> "long";
            case "gdouble", "long double" -> "double";
            case "gfloat" -> "float";
            case "none" -> "void";
            case "utf8", "filename" -> "String";
            case "gpointer", "gconstpointer",
                 // treat va_list as an opaque pointer
                 "valist", "va_list" -> "MemorySegment";
            case "gtype" -> "org.gnome.glib.GType";
            default -> null;
        };
    }

    /**
     * Return the TypeName for the given primitive type.
     */
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
            default -> throw new IllegalStateException("Unexpected value: %s"
                    .formatted(primitive));
        };
    }

    /**
     * Convert "char" to "Character", "int" to "Integer", and capitalize all
     * other primitive types.
     */
    public static String primitiveClassName(String primitive) {
        return switch(primitive) {
            case "char" -> "Character";
            case "int" -> "Integer";
            default -> capitalize(primitive);
        };
    }

    /**
     * Return the TypeName of the carrier type.
     */
    public static TypeName getCarrierTypeName(AnyType t, boolean longAsInt) {
        if (t == null || t instanceof Array)
            return TypeName.get(MemorySegment.class);

        Type type = (Type) t;

        if (type.isPointer())
            return TypeName.get(MemorySegment.class);

        if (type.isBoolean())
            return TypeName.INT;

        if (type.isLong() && longAsInt)
            return TypeName.INT;

        if (type.isPrimitive() || "none".equals(t.cType()))
            return type.typeName();

        RegisteredType target = type.lookup();

        if (target instanceof EnumType)
            return TypeName.INT;

        if (target instanceof Alias a && a.isValueWrapper())
            return a.anyType().typeName();

        return TypeName.get(MemorySegment.class);
    }

    /**
     * Return a type tag that can be used for the carrier type.
     */
    public static String getCarrierTypeTag(AnyType t) {
        if (t == null || t instanceof Array)
            return "memorySegment";

        Type type = (Type) t;

        if (type.isPointer())
            return "memorySegment";

        if (type.isBoolean())
            return "int";

        if (type.isPrimitive() || "none".equals(t.cType()))
            return toJavaBaseType(type.name());

        RegisteredType target = type.lookup();

        if (target instanceof EnumType)
            return "int";

        if (target instanceof Alias a && a.isValueWrapper())
            return getCarrierTypeTag(a.anyType());

        return "memorySegment";
    }

    /**
     * Get the memory layout of this type. Pointer types are returned as
     * "ADDRESS".
     */
    public static String getValueLayout(AnyType anyType, boolean longAsInt) {
        return switch (anyType) {
            case null -> "ADDRESS";
            case Array _ -> "ADDRESS";
            case Type t -> t.isPointer() ? "ADDRESS"
                                         : getValueLayoutPlain(t, longAsInt);
        };
    }

    /**
     * Get the memory layout of this type. Pointers to primitive types are
     * treated as the actual type.
     */
    public static String getValueLayoutPlain(Type t, boolean longAsInt) {
        if (t == null) {
            return "ADDRESS";
        }
        RegisteredType target = t.lookup();
        if (target instanceof EnumType || t.isBoolean()) {
            return "JAVA_INT";
        }
        if (t.isLong() && longAsInt) {
            return "JAVA_INT";
        }
        if (t.isPrimitive()) {
            return "JAVA_" + t.javaType().toUpperCase();
        }
        if (target instanceof Alias a && a.isValueWrapper()
                && a.anyType() instanceof Type type) {
            return getValueLayoutPlain(type, longAsInt);
        }
        return "ADDRESS";
    }

    /**
     * Generate the literal representation of the provided value, according
     * to the provided type.
     */
    public static String literal(TypeName type, String value)
            throws NumberFormatException {

        if (type.equals(TypeName.BOOLEAN))
            return Boolean.valueOf(value).toString();

        if (type.equals(TypeName.BYTE))
            return Numbers.parseByte(value).toString();

        if (type.equals(TypeName.CHAR))
            return "'" + value + "'";

        if (type.equals(TypeName.DOUBLE))
            return Double.valueOf(value) + "d";

        if (type.equals(TypeName.FLOAT))
            return Float.valueOf(value) + "f";

        if (type.equals(TypeName.INT))
            return Numbers.parseInt(value).toString();

        if (type.equals(TypeName.LONG))
            return Numbers.parseLong(value) + "L";

        if (type.equals(TypeName.SHORT))
            return Numbers.parseShort(value).toString();

        if (type.equals(TypeName.get(String.class)))
            return '"' + value.replace("\\", "\\\\") + '"';

        return value;
    }
}
