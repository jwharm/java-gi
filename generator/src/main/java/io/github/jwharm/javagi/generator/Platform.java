package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.model.Namespace;

public class Platform {

    private static final String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return OS.contains("windows");
    }

    static String convertLinuxToJavaType(String name, boolean qualified, Namespace ns) {
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
            case "gtype" -> qualified ? Conversions.toQualifiedJavaType("GLib.Type", ns) : Conversions.toSimpleJavaType("GLib.Type", ns);
            case "valist", "va_list" -> "VaList";
            case "long double" -> "double"; // unsupported data type
            default -> qualified ? Conversions.toQualifiedJavaType(name, ns) : Conversions.toSimpleJavaType(name, ns);
        };
    }

    static String convertWindowsToJavaType(String name, boolean qualified, Namespace ns) {
        return name == null ? null : switch (name.toLowerCase()) {
            case "gboolean" -> "boolean";
            case "gchar", "guchar", "gint8", "guint8" -> "byte";
            case "gshort", "gushort", "gint16", "guint16" -> "short";
            case "gint", "guint", "gint32", "guint32", "gunichar", "glong", "gulong" -> "int";
            case "gint64", "gssize", "gsize", "goffset", "guint64", "gintptr", "guintptr" -> "long";
            case "gdouble" -> "double";
            case "gfloat" -> "float";
            case "none" -> "void";
            case "utf8", "filename" -> "java.lang.String";
            case "gpointer", "gconstpointer" -> "java.lang.foreign.MemoryAddress";
            case "gtype" -> qualified ? Conversions.toQualifiedJavaType("GLib.Type", ns) : Conversions.toSimpleJavaType("GLib.Type", ns);
            case "valist", "va_list" -> "VaList";
            case "long double" -> "double"; // unsupported data type
            default -> qualified ? Conversions.toQualifiedJavaType(name, ns) : Conversions.toSimpleJavaType(name, ns);
        };
    }
}
