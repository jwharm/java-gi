package io.github.jwharm.javagi.generator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.jwharm.javagi.model.*;

public class Conversions {

    /**
     * Map to find java packages by namespaces
     */
    public static final Map<String, String> nsLookupTable = new HashMap<>();
    /**
     * Map to find elements by their {@code c:identifier} attribute
     */
    public static final Map<String, GirElement> cIdentifierLookupTable = new HashMap<>();
    /**
     * Map to find types by their {@code c:type} attribute
     */
    public static final Map<String, RegisteredType> cTypeLookupTable = new HashMap<>();
    /**
     * Map to find repositories by their name
     */
    public static final Map<String, Repository> repositoriesLookupTable = new HashMap<>();
    /**
     * Map to find parent types by a types qualified name
     */
    public static final Map<String, String> superLookupTable = new HashMap<>();

    /**
     * Convert "Gdk" to "org.gnome.gdk"
     */
    public static String namespaceToJavaPackage(String ns) {
        return Objects.requireNonNullElse(nsLookupTable.get(ns.toLowerCase()), ns);
    }

    /** 
     * Convert "identifier_name" to "identifierName"
     */
    public static String toLowerCaseJavaName(String typeName) {
        return prefixDigits(replaceKeywords(toCamelCase(typeName, false)));
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
            Repository repo = repositoriesLookupTable.get(nsPrefix);
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
     * For types that conflict with common Java classes, prefix with Gi
     */
    public static String replaceKnownType(String name, Namespace ns) {
        final String[] types = {"String", "Object", "Builder"};
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
            case "glong", "gulong", "gint64", "gssize", "gsize", "goffset", "guint64", "gintptr", "guintptr" -> "long";
            case "gdouble" -> "double";
            case "gfloat" -> "float";
            case "none" -> "void";
            case "utf8", "filename" -> "java.lang.String";
            case "gpointer", "gconstpointer" -> "java.lang.foreign.MemoryAddress";
            case "gtype" -> qualified ? toQualifiedJavaType("GLib.Type", ns) : toSimpleJavaType("GLib.Type", ns);
            case "valist", "va_list" -> "VaList";
            case "long double" -> "double"; // unsupported data type
            default -> qualified ? toQualifiedJavaType(name, ns) : toSimpleJavaType(name, ns);
        };
    }

    /**
     * Get the type name to use when interfacing with native code
     */
    public static String toPanamaJavaType(Type t) {
        if (t == null) {
            return "MemoryAddress";
        } else if (t.isPointer()) {
            return "MemoryAddress";
        } else if (t.isEnum() || t.isBitfield() || t.isBoolean()) {
            return "int";
        } else if (t.isPrimitive || "void".equals(t.simpleJavaType)) {
            return t.simpleJavaType;
        } else if (t.isAliasForPrimitive()) {
            return t.girElementInstance.type.simpleJavaType;
        } else {
            return "MemoryAddress";
        }
    }

    public static String getMarshal(Type t) {
        if (t == null) {
            return "Marshal.passthrough";
        } else if ("java.lang.foreign.MemoryAddress".equals(t.qualifiedJavaType)) {
            return "Marshal.passthrough";
        } else if ("java.lang.String".equals(t.qualifiedJavaType)) {
            return "Marshal.stringToAddress";
        } else if (t.cType != null && t.cType.endsWith("*")) {
            return "Marshal.passthrough";
        } else if (t.isBoolean()) {
            return "Marshal.booleanToInteger";
        } else if (t.isEnum()) {
            return "Marshal.enumerationToInteger";
        } else if (t.isBitfield()) {
            return "Marshal.bitfieldToInteger";
        } else if (t.isPrimitive || "void".equals(t.simpleJavaType)) {
            return "Marshal.passthrough";
        } else if (t.isAliasForPrimitive()) {
            return "Marshal.aliasToPrimitive";
        } else if (t.isCallback()) {
            return "Marshal.callbackToAddress";
        } else {
            return t.qualifiedJavaType + ".fromAddress";
        }
    }

    /**
     * Get the memory layout of this type. Pointer types are returned as Interop.valueLayout.ADDRESS.
     */
    public static String toPanamaMemoryLayout(Type t) {
        if (t == null) {
            return "Interop.valueLayout.ADDRESS";
        } else if (t.isEnum() || t.isBitfield() || t.isBoolean()) {
            return "Interop.valueLayout.C_INT";
        } else if (t.isPointer()) {
            return "Interop.valueLayout.ADDRESS";
        } else if (t.isPrimitive) {
            return "Interop.valueLayout.C_" + t.simpleJavaType.toUpperCase();
        } else if (t.isAliasForPrimitive()) {
            return toPanamaMemoryLayout(t.girElementInstance.type);
        } else {
            return "Interop.valueLayout.ADDRESS";
        }
    }

    /**
     * Get the memory layout of this type. Pointer types are treated as the actual type.
     */
    public static String getValueLayout(Type t) {
        if (t == null) {
            return "Interop.valueLayout.ADDRESS";
        } else if (t.isEnum() || t.isBitfield() || t.isBoolean()) {
            return "Interop.valueLayout.C_INT";
        } else if (t.isPrimitive) {
            return "Interop.valueLayout.C_" + t.simpleJavaType.toUpperCase();
        } else if (t.isAliasForPrimitive()) {
            return getValueLayout(t.girElementInstance.type);
        } else {
            return "Interop.valueLayout.ADDRESS";
        }
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
            case "java.lang.foreign.MemoryAddress" -> "Address";
            case "java.lang.String" -> "String";
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
