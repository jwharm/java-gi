package girparser.generator;

import girparser.model.Type;

import java.util.Arrays;

public class Conversions {

    /** Convert "Gdk" to "org.gtk.gdk" */
    public static String namespaceToJavaPackage(String ns) {
        String packageName;
        switch (ns.toLowerCase()) {
            case "gobject", "glib", "gio", "atk", "gdk", "gsk", "gdkpixbuf", "graphene", "gtk" -> {
                packageName = "org.gtk." + ns;
            }
            case "cairo" -> {
                packageName = "org.cairographics";
            }
            case "pango", "pangocairo", "harfbuzz" -> {
                packageName = "org." + ns;
            }
            default -> {
                packageName = ns;
            }
        }
        return packageName.toLowerCase();
    }

    /** Convert "identifier_name" to "identifierName" */
    public static String toLowerCaseJavaName(String typeName) {
        return replaceKeywords(toCamelCase(typeName, false));
    }

    /** Convert "GLib.type_name" to "TypeName" */
    public static String toSimpleJavaType(String typeName) {
        if (typeName == null) {
            return null;
        }
        int idx = typeName.indexOf('.');
        if (idx > 0) {
            return toCamelCase(typeName.substring(idx + 1), true);
        } else {
            return toCamelCase(typeName, true);
        }
    }

    /** Convert "Glib.type_name" to "org.gtk.glib.TypeName" */
    public static String toQualifiedJavaType(String typeName) {
        if (typeName == null) {
            return null;
        }
        int idx = typeName.indexOf('.');
        if (idx > 0) {
            return Conversions.namespaceToJavaPackage(typeName.substring(0, idx))
                    + "." + toCamelCase(typeName.substring(idx + 1), true);
        } else {
            return toCamelCase(typeName, true);
        }
    }

    /** Convert "GLib.TypeName" to "org.gtk.glib" */
    public static String getJavaPackageName(String typeName) {
        if (typeName == null) {
            return null;
        }
        int idx = typeName.indexOf('.');
        if (idx > 0) {
            return "org.gtk." + typeName.substring(0, idx).toLowerCase();
        }
        return null;
    }

    private static String toCamelCase(String typeName, boolean startUpperCase) {
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

    public static String prefixDigits(String name) {
        return Character.isDigit(name.charAt(0)) ? "_" + name : name;
    }

    private static String replaceKeywords(String name) {
        final String[] keywords = new String[] {
                "abstract", "continue", "for", "new", "switch", "assert", "default", "goto", "package",
                "synchronized", "boolean", "do", "if", "private", "this", "break", "double", "implements",
                "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum",
                "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char",
                "final", "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile",
                "const", "float", "native", "super", "while"
        };
        return (Arrays.stream(keywords).anyMatch(kw -> kw.equalsIgnoreCase(name))) ? name + "_" : name;
    }

    /** Convert C type declaration into Java type declaration */
    public static String convertToJavaType(String name, boolean qualified) {
        return name == null ? null : switch (name.toLowerCase()) {
            case "gboolean" -> "boolean";
            case "gchar", "guchar", "gint8", "guint8" -> "byte";
            case "gshort", "gushort", "gint16", "guint16" -> "short";
            case "gint", "guint", "gint32", "guint32", "gunichar" -> "int";
            case "glong", "gulong", "gint64", "gssize", "gsize", "goffset", "guint64" -> "long";
            case "gdouble" -> "double";
            case "gfloat" -> "float";
            case "none" -> "void";
            case "utf8", "filename" -> "java.lang.String";
            case "gpointer", "gconstpointer" -> "jdk.incubator.foreign.MemoryAddress";
            case "gtype" -> qualified ? toQualifiedJavaType("GObject.GType") : toSimpleJavaType("GObject.GType");
            default -> qualified ? toQualifiedJavaType(name) : toSimpleJavaType(name);
        };
    }

    public static String toPanamaJavaType(Type t) {
        if (t == null) {
            return "MemoryAddress";
        } else if (t.isEnum() || t.isBitfield()) {
            return "int";
        } else if (t.isPrimitive) {
            return t.simpleJavaType;
        } else {
            return "MemoryAddress";
        }
    }

    public static String toPanamaMemoryLayout(Type t) {
        if (t == null) {
            return "ValueLayout.ADDRESS";
        } else if (t.isEnum() || t.isBitfield()) {
            return "ValueLayout.JAVA_INT";
        } else if (t.isPrimitive) {
            return "ValueLayout.JAVA_" + t.simpleJavaType.toUpperCase();
        } else {
            return "ValueLayout.ADDRESS";
        }
    }

    public static boolean isPrimitive(String javaType) {
        return javaType != null && switch (javaType) {
            case "boolean", "long", "byte", "short", "int", "double", "float" -> true;
            default -> false;
        };
    }
}